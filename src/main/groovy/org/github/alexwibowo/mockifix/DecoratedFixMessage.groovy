package org.github.alexwibowo.mockifix

import com.google.common.base.Preconditions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quickfix.FixVersions
import quickfix.field.converter.UtcTimestampConverter

import java.text.DecimalFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * User: alexwibowo
 */
public class DecoratedFixMessage {

    public static final Logger LOGGER = LoggerFactory.getLogger(DecoratedFixMessage.class.getName());

    // Matches FIX.X.X or FIXT.X.X style begin string
    private static final Pattern MESSAGE_PATTERN = Pattern
            .compile("(8=FIXT?\\.\\d\\.\\d\\001)(.*?\\001)(10=.*|)\$");

    private static final Pattern MESSAGE_BODY_PATTERN = Pattern
            .compile("(8=FIXT?\\.\\d\\.\\d\\001)(9=\\d+\001)(.*?\\001)(10=.*|)\$");

    private static final Pattern EVERYTHING_EXCLUDING_CHECKSUM_PATTERN = Pattern
            .compile("(.*?\\001)(10=.*|)\$");

    private static final Pattern TIME_PATTERN = Pattern.compile("<TIME([+-](\\d+))*>");

    private static final Pattern HEARTBEAT_PATTERN = Pattern.compile("108=\\d+\001");


    private static final DecimalFormat CHECKSUM_FORMAT = new DecimalFormat("000");

    private static int heartBeatOverride = -1;

    static {
        String hbi = System.getProperty("atest.heartbeat");
        if (hbi != null) {
            heartBeatOverride = Integer.parseInt(hbi);
        }
    }

    private String decoratedMessage;

    private int clientId;

    public DecoratedFixMessage(String data) {
        processMessage(data);
    }

    public int getClientId() {
        return clientId;
    }

    /**
     * Change the sequence number in the message. Calling this method will also recalculate the body length and the checksum
     * <p/>
     * @param sequenceNumber new sequence number
     */
    public DecoratedFixMessage setSequenceNumber(long sequenceNumber) {
        if (!this.decoratedMessage.contains("\00134=")) {
            LOGGER.debug("Sequence number was not specified. Appending sequence number:" + sequenceNumber);
            decoratedMessage=decoratedMessage.replaceFirst("\00135=.", "\$0\00134=" + sequenceNumber);
        }else {
            String newField = "\00134=" + sequenceNumber;
            decoratedMessage=decoratedMessage.replaceFirst("\00134=(.*?)\001", newField + "\001");
        }

        // must recalculate body length, as sequence has changed
        populateBodyLength();

        // recalculate checksum
        calculateChecksum();
        return this;
    }

    public String toString() {
        return decoratedMessage;
    }


    private void processMessage(String data) {
        Matcher messageStructureMatcher = MESSAGE_PATTERN.matcher(data);
        String message;
        if (messageStructureMatcher.matches()) {
           /* if (messageStructureMatcher.group(1) != null
                    && !messageStructureMatcher.group(1).equals("")) {
                clientId = Integer.parseInt(messageStructureMatcher.group(1).replaceAll(",", ""));
            } else {*/
                clientId = 1;
//            }
            String version = messageStructureMatcher.group(1);
            String messageTail = insertTimes(messageStructureMatcher.group(2));
            messageTail = modifyHeartbeat(messageTail);
            String checksum = messageStructureMatcher.group(3);
            if ("10=0\001".equals(checksum)) {
                checksum = "10=000\001";
            }

            message = version + (!messageTail.startsWith("9=") ? "9=" + messageTail.length() + "\001" : "") + messageTail + checksum;
        } else {
            LOGGER.info("garbled message being sent");
            clientId = 1;
            message = data.substring(1);
        }
        this.decoratedMessage = message.replaceAll("\\s+", "");

        populateBodyLength();

        calculateChecksum();
    }

    private void populateBodyLength() {
        Matcher messageStructureMatcher = MESSAGE_BODY_PATTERN.matcher(decoratedMessage);
        if (messageStructureMatcher.matches()) {
            String messageTail = messageStructureMatcher.group(3);
            int newBodyLength = messageTail.length();
            decoratedMessage = decoratedMessage.replaceFirst("\0019(.*?)\001", "\0019=" + newBodyLength + "\001");
        }
    }

    private void calculateChecksum() {
        Matcher everythingExcludingChecksumMatcher = EVERYTHING_EXCLUDING_CHECKSUM_PATTERN.matcher(decoratedMessage);
        if (everythingExcludingChecksumMatcher.matches()) {
            String message = everythingExcludingChecksumMatcher.group(1);
            String calculatedChecksum = CHECKSUM_FORMAT.format(checksum(message));
            if (!this.decoratedMessage.contains("\00110=")) {
                this.decoratedMessage += "10=" + calculatedChecksum + '\001';
            }else{
                this.decoratedMessage=decoratedMessage.replaceFirst("\00110=(.*?)\001", "\00110=" + calculatedChecksum + "\001");
            }
        }
    }

    private String insertTimes(String message) {
        Matcher matcher = TIME_PATTERN.matcher(message);
        while (matcher.find()) {
            long offset = 0;
            if (matcher.group(2) != null) {
                offset = Long.parseLong(matcher.group(2)) * 1100L;
                if (matcher.group(1).startsWith("-")) {
                    offset *= -1;
                }
            }
            String beginString = message.substring(2, 9);
            boolean includeMillis = beginString.compareTo(FixVersions.BEGINSTRING_FIX42) >= 0;
            message = matcher.replaceFirst(UtcTimestampConverter.convert(new Date(System
                    .currentTimeMillis()
                    + offset), includeMillis));
            matcher = TIME_PATTERN.matcher(message);
        }
        return message;
    }

    private String modifyHeartbeat(String messageTail) {
        if (heartBeatOverride > 0 && messageTail.contains("35=A\001")) {
            Matcher matcher = HEARTBEAT_PATTERN.matcher(messageTail);
            if (matcher.find()) {
                return matcher.replaceFirst("108=" + heartBeatOverride + "\001");
            }
        }
        return messageTail;
    }

    private int checksum(String message) {
        int sum = 0;
        int fieldSum = 0;
        for (int i = 0; i < message.length(); i++) {
            sum += message.charAt(i);
            fieldSum += message.charAt(i);
            if (message.charAt(i) == '\001') {
                fieldSum = 0;
            }
        }
        return sum % 256;
    }
}