package io.spring.sftp.lister;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ConfigurationProperties class for SftpListerSourceConfiguration.
 *
 * @author Chris Schaefer
 */
@ConfigurationProperties
public class SftpListerSourceProperties {
	private static final Integer DEFAULT_SFTP_PORT = 22;

	/**
	 * The SFTP host.
	 */
	private String host;

	/**
	 * The (optional) SFTP port, defaults to 22.
	 */
	private Integer port = DEFAULT_SFTP_PORT;

	/**
	 * The SFTP username.
	 */
	private String username;

	/**
	 * The SFTP password.
	 */
	private String password;

	/**
	 * The remote directory on the SFTP server.
	 */
	private String remoteDir;

	/**
	 * Allow unknown keys?
	 */
	private boolean allowUnknownKeys;

	@NotBlank
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Range(min = 0, max = 65535)
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@NotBlank
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@NotBlank
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@NotBlank
	public String getRemoteDir() {
		return remoteDir;
	}

	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public boolean isAllowUnknownKeys() {
		return this.allowUnknownKeys;
	}

	public void setAllowUnknownKeys(boolean allowUnknownKeys) {
		this.allowUnknownKeys = allowUnknownKeys;
	}
}
