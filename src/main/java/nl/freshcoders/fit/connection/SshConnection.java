package nl.freshcoders.fit.connection;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import nl.freshcoders.fit.target.Target;

import java.io.InputStream;
import java.util.Optional;

/**
 * Remotely execute commands through SSH. **Currently unused**
 */
public class SshConnection extends RemoteConnection<Session> {

    public SshConnection(Target target) {
        this(target.getHost(), target.getPort());
    }

    public SshConnection(String ip, Integer port) {
        super(ip, port);
    }

    public Optional<Session> setupConnection() {
        Session session;
        try{
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            jsch.addIdentity("/Users/nickdek/.sandbox-cli/.sandbox_rsa");
            session=jsch.getSession("sandbox", ip, port);
            session.setConfig(config);
            session.connect();
            if (session.isConnected())
               return Optional.of(session);
        }catch(Exception e){
            e.printStackTrace();
        }
        return Optional.empty();
    }


    private void executeCommand(Session session, String command) {
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            System.out.println("DONE");
        } catch (Exception e) {
            //..
        }
    }


    public boolean run(String command) {
        Session sshSession = getConnection().orElseThrow();
        if (sshSession == null) {
            throw new RuntimeException("Could not establish SSH session.");
        }
        executeCommand(sshSession, command);
        sshSession.disconnect();
        return true;
    }

    @Override
    public boolean isOpen() {
        return connection.map(Session::isConnected).orElse(false);
    }

    @Override
    public void close() {
        connection.orElseThrow().disconnect();
    }

    // TODO: on destruct method clean up existing SSH connections
}
