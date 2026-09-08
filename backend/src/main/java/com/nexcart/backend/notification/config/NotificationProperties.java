package com.nexcart.backend.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

	private String fromAddress;
}
