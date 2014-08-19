package org.github.alexwibowo.mockifix

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quickfix.*
import quickfix.field.AvgPx
import quickfix.field.BeginString
import quickfix.field.BusinessRejectReason
import quickfix.field.ClOrdID
import quickfix.field.CumQty
import quickfix.field.ExecID
import quickfix.field.LastPx
import quickfix.field.LastShares
import quickfix.field.LeavesQty
import quickfix.field.MsgSeqNum
import quickfix.field.MsgType
import quickfix.field.OrdStatus
import quickfix.field.RefMsgType
import quickfix.field.RefSeqNum
import quickfix.field.SenderCompID
import quickfix.field.Symbol
import quickfix.field.TargetCompID
import quickfix.field.Text

/**
 * User: alexwibowo
 */

public class Application implements quickfix.Application {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private DefaultMessageFactory messageFactory = new DefaultMessageFactory();

    static private HashMap<SessionID, HashSet<ExecID>> execIDs = new HashMap<SessionID, HashSet<ExecID>>();

    public void onCreate(SessionID sessionID) {
    }

    public void onLogon(SessionID sessionID) {
    }

    public void onLogout(SessionID sessionID) {
    }

    public void toAdmin(Message message, SessionID sessionID) {
    }

    public void toApp(Message message, SessionID sessionID)
            throws DoNotSend {
    }

    public void fromAdmin(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat,
                    IncorrectTagValue, RejectLogon {
    }

    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        new MessageProcessor(message, sessionID).run()
    }

    private Message createMessage(Message message, String msgType) throws FieldNotFound {
        return messageFactory.create(message.getHeader().getString(BeginString.FIELD), msgType);
    }

    private void reverseRoute(Message message, Message reply) throws FieldNotFound {
        reply.getHeader().setString(SenderCompID.FIELD,
                message.getHeader().getString(TargetCompID.FIELD));
        reply.getHeader().setString(TargetCompID.FIELD,
                message.getHeader().getString(SenderCompID.FIELD));
    }

    private void sendBusinessReject(Message message, int rejectReason, String rejectText)
            throws FieldNotFound, SessionNotFound {
        Message reply = createMessage(message, MsgType.BUSINESS_MESSAGE_REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
        reply.setString(RefSeqNum.FIELD, refSeqNum);
        reply.setString(RefMsgType.FIELD, message.getHeader().getString(MsgType.FIELD));
        reply.setInt(BusinessRejectReason.FIELD, rejectReason);
        reply.setString(Text.FIELD, rejectText);
        Session.sendToTarget(reply);
    }

    private boolean alreadyProcessed(ExecID execID, SessionID sessionID) {
        HashSet<ExecID> set = execIDs.get(sessionID);
        if (set == null) {
            set = new HashSet<ExecID>();
            set.add(execID);
            execIDs.put(sessionID, set);
            return false;
        } else {
            if (set.contains(execID))
                return true;
            set.add(execID);
            return false;
        }
    }

    private void executionReport(Message message, SessionID sessionID) throws FieldNotFound {
        ExecID execID = (ExecID) message.getField(new ExecID());
        if (alreadyProcessed(execID, sessionID))
            return;

        LeavesQty leavesQty = new LeavesQty()
        message.getField(leavesQty)
    }

    public class MessageProcessor implements Runnable {
        private Message message;
        private SessionID sessionID;

        public MessageProcessor(Message message, SessionID sessionID) {
            this.message = message;
            this.sessionID = sessionID;
        }

        public void run() {
            try {
                MsgType msgType = new MsgType();
                if (message.getHeader().getField(msgType).valueEquals("8")) {
                    executionReport(message, sessionID);
                } else {
                    sendBusinessReject(message, BusinessRejectReason.UNSUPPORTED_MESSAGE_TYPE,
                            "Unsupported Message Type");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
