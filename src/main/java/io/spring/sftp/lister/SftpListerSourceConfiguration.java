package io.spring.sftp.lister;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Source to obtain new file locations that appear on the configured
 * SFTP host.
 *
 * @author Chris Schaefer
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties(SftpListerSourceProperties.class)
public class SftpListerSourceConfiguration {
	private static final String REMOTE_FILE_MESSAGE_HEADER = "file_remoteFile";
	private static final String REMOTE_DIRECTORY_MESSAGE_HEADER = "file_remoteDirectory";

	private final BeanFactory beanFactory;
	private final SftpListerSourceProperties sftpListerSourceProperties;

	@Autowired
	public SftpListerSourceConfiguration(final BeanFactory beanFactory,
							final SftpListerSourceProperties sftpListerSourceProperties) {
		this.beanFactory = beanFactory;
		this.sftpListerSourceProperties = sftpListerSourceProperties;
	}

	@Bean
	public MessageChannel incomingSftpChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel outboundSftpChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel discardChannel() {
		return new NullChannel();
	}

	@Bean
	@InboundChannelAdapter(value = "incomingSftpChannel", poller = @Poller(fixedDelay = "2000"))
	public MessageSource sftpMessageSource() {
		RemoteFileTemplate<com.jcraft.jsch.ChannelSftp.LsEntry> remoteFileTemplate
			= new SftpRemoteFileTemplate(getSessionFactory());

		String remoteDir = sftpListerSourceProperties.getRemoteDir();

		if (!remoteDir.endsWith("/")) {
			remoteDir = remoteDir + "/";
		}

		SftpStreamingMessageSource sftpStreamingMessageSource = new SftpStreamingMessageSource(remoteFileTemplate);
		sftpStreamingMessageSource.setRemoteDirectoryExpression(new LiteralExpression(remoteDir));

		return sftpStreamingMessageSource;
	}

	@Bean
	@ServiceActivator(inputChannel = "incomingSftpChannel")
	@IdempotentReceiver("idempotentReceiverInterceptor")
	public MessageHandler incomingSftpMessageHandler() {
		SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(getSessionFactory(),
									Command.LS.getCommand(),
									"headers['" + REMOTE_DIRECTORY_MESSAGE_HEADER + "']");
		sftpOutboundGateway.setOutputChannel(outboundSftpChannel());
		sftpOutboundGateway.setOptions("-1");

		return sftpOutboundGateway;
	}

	@Filter(inputChannel = "outboundSftpChannel", outputChannel = "output")
	public boolean sftpFileFilter(Message<?> message) {
		String filename = (String) message.getHeaders().get(REMOTE_FILE_MESSAGE_HEADER);

		return filename.equals("..") ? false : true;
	}

	@Bean
	public IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
		ConcurrentMetadataStore store = new SimpleMetadataStore();

		String expressionStatement = new StringBuilder()
			.append("headers['")
			.append(REMOTE_DIRECTORY_MESSAGE_HEADER)
			.append("'].concat(headers['")
			.append(REMOTE_FILE_MESSAGE_HEADER)
			.append("'])")
			.toString();

		Expression expression = new SpelExpressionParser().parseExpression(expressionStatement);

		ExpressionEvaluatingMessageProcessor<String> idempotentKeyStrategy =
				new ExpressionEvaluatingMessageProcessor<>(expression);
		idempotentKeyStrategy.setBeanFactory(beanFactory);

		IdempotentReceiverInterceptor idempotentReceiverInterceptor =
				new IdempotentReceiverInterceptor(new MetadataStoreSelector(idempotentKeyStrategy, store));
		idempotentReceiverInterceptor.setDiscardChannel(discardChannel());

		return idempotentReceiverInterceptor;
	}

	private DefaultSftpSessionFactory getSessionFactory() {
		DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
		sessionFactory.setHost(sftpListerSourceProperties.getHost());
		sessionFactory.setPort(sftpListerSourceProperties.getPort());
		sessionFactory.setUser(sftpListerSourceProperties.getUsername());
		sessionFactory.setPassword(sftpListerSourceProperties.getPassword());
		sessionFactory.setAllowUnknownKeys(sftpListerSourceProperties.isAllowUnknownKeys());

		return sessionFactory;
	}
}
