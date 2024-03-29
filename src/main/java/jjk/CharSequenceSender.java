package jjk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;

public class CharSequenceSender {

    private static final Integer THREAD_POOL_SIZE = 100;

    protected final Endpoint clientEndpoint;

    protected final Registration clientReg;

    protected final Connection conn;

    public CharSequenceSender() throws IOException {
        clientEndpoint = Remoting.createEndpoint("connection-test-client", OptionMap.create(Options.WORKER_TASK_CORE_THREADS, THREAD_POOL_SIZE, Options.WORKER_TASK_MAX_THREADS, THREAD_POOL_SIZE));
        clientReg = clientEndpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));
        AuthenticationContext context = AuthenticationContext.empty();
        AuthenticationConfiguration commonConfig =
        AuthenticationConfiguration.empty()
          //.useProviders(() -> new Provider[] { new WildFlyElytronProvider() })
          .setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("PLAIN"))
          .useRealm("ManagementRealm");
        AuthenticationConfiguration administrator =
            commonConfig
              .useName("admin") //Example credentials
              .usePassword("admin");
        context = context.with(MatchRule.ALL.matchHost("<host>"), administrator);
        conn = clientEndpoint.connect(URI.create("remote://<host>:4444"), new InetSocketAddress("0.0.0.0", 0), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE), context).get();
        System.out.println("Connection created, " + conn.getEndpoint().getName());
        send();
    }


    private void send() {
        System.out.println("enter 'quit' or 'exit' exit the ChatDemo");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                String line = in.readLine().toLowerCase();
                if (line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }

                MessageOutputStream stream = null;
                Channel channel = null;

                try {
                    final IoFuture<Channel> future = conn.openChannel("test", OptionMap.EMPTY);
                    channel = future.get();
                    stream = channel.writeMessage();
                    stream.write(new String(line.getBytes(), "UTF-8").getBytes());
                } catch (Exception e) {
                    throw new RuntimeException("send char sequence error", e);
                } finally {
                    stream.close();
                    IoUtils.safeClose(channel);
                }

            } catch (Exception e) {
                throw new RuntimeException("send char sequence error", e);
            }
        }

        IoUtils.safeClose(clientReg);
        IoUtils.safeClose(clientEndpoint);
    }


    public static void main(String[] args) throws IOException {
        new CharSequenceSender();
    }

}
