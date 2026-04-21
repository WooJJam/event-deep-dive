package co.kr.woojjam.event_deep_dive.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class InstanceIdProvider {

    @Bean("instanceId")
    public String instanceId() throws UnknownHostException {
        String envId = System.getenv("APP_INSTANCE_ID");
        return (envId != null && !envId.isBlank())
                ? envId
                : InetAddress.getLocalHost().getHostName();
    }
}
