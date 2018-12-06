package ertp.com.jlibrtp;

import java.net.DatagramSocket;

public class RtpSender implements RTPAppIntf
{

    public RTPSession rtpSession;

    public RtpSender(Participant remoteParticipant) {

        DatagramSocket rtpSocket = null;
        DatagramSocket rTcpSocket = null;

        try {
            rtpSocket = new DatagramSocket(16386);
            rTcpSocket = new DatagramSocket(16387);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        rtpSession = new RTPSession(rtpSocket, rTcpSocket);
        rtpSession.RTPSessionRegister(this,null, null);
        System.out.println("CNAME: " + rtpSession.CNAME());


    }

    // TODO: 添加接收语音方
    public void addParicipants(String[] paricipants) {
        if (paricipants.length > 0) {
            for (String pa: paricipants) {
                Participant remoteParticipant = new Participant(pa, 18888, 18889);
                rtpSession.addParticipant(remoteParticipant);
            }
        }
    }

    // TODO: 添加接收方
    public void addParicipants(String iPAddress) {
        Participant remoteParticipant = new Participant(iPAddress, 18888, 18889);
        rtpSession.addParticipant(remoteParticipant);
    }

    public RtpSender() {
        this(null);
    }


    @Override
    public void receiveData(DataFrame frame, Participant participant) {

    }

    @Override
    public void userEvent(int type, Participant[] participant) {

    }

    @Override
    public int frameSize(int payloadType) {
        return 1;
    }
}
