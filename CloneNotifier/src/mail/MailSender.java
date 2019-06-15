package mail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import cn.Def;
import cn.Logger;
import cn.SettingFileLoader;
import cn.data.Account;
import cn.data.Project;

/**
 * <p>
 * 電子メール送信クラス
 * </p>
 *
 * @author y-yuuki
 */
public class MailSender {

	/**
	 * <p>
	 * メール送信
	 * </p>
	 *
	 */
	public static void main(String[] args) {
		// ログファイル初期化
		try {
			Logger.init();
		} catch (IOException e) {
			Logger.printlnConsole("Can't generate log file.", Logger.ERROR);
			System.exit(1);
		}

		// CloneNotifierのパス設定

		Path path = null;
		try {
			path = Paths.get(MailSender.class.getClassLoader().getResource("").toURI());
		} catch (URISyntaxException e) {
			Logger.writeError(e);
			System.exit(1);
		}
		if (path.endsWith("bin"))
			path = path.getParent();
		Def.NOTIFIER_PATH = path.toString();

		// send Email
		for (String arg : args) {
			Project project = new Project();
			if (SettingFileLoader.loadSettingFile(arg, project)) {
				Logger.printlnConsole("Load setting file.", Logger.SYSTEM);
				Logger.writeln("<Success> Load setting file.", Logger.SYSTEM);
			} else {
				Logger.writeln("Can't load setting file.", Logger.ERROR);
				continue;
			}

			if ((new File(project.getGenerateTextDir())).exists()) {
				Logger.writeln("<Success> Load result file.", Logger.SYSTEM);
			} else {
				Logger.writeln("Can't load result file.", Logger.ERROR);
				Logger.writeln("Please execute CloneNotifier.", Logger.ERROR);
				continue;
			}
			Logger.printlnConsole("send email.", Logger.SYSTEM);
			setting(project);
		}
	}

	private static void setting(Project project) {

		// textFileNameのファイル名生成
		Calendar now = Calendar.getInstance();
		/** 日付フィールド. 年 */
		String year;
		year = Integer.toString(now.get(Calendar.YEAR));
		/** 日付フィールド. 月 */
		String month;
		// 月は2桁にする
		month = Integer.toString(now.get(Calendar.MONTH) + 1);
		if (month.length() == 1) {
			month = "0" + month;
		}
		/** 日付フィールド. 日 */
		String day;
		// 日も2桁にする
		day = Integer.toString(now.get(Calendar.DATE));
		if (day.length() == 1) {
			day = "0" + day;
		}
		String textFileName = project.getGenerateTextDir() + "//" + project.getName() + year + month + day + ".txt";

		try {
			Properties props = new Properties();
			props.put("mail.smtp.host", project.getHost());
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.port", String.valueOf(project.getPort()));

			if (project.getSsl() == 2) {
				props.put("mail.smtp.starttls.enable", "true");
			} else if (project.getSsl() == 1) {
				props.put("mail.smtp.ssl.enable", "true");
			}

			// アカウント/パスワードの復号
			Decoder dec = new Decoder();
			final Account account = dec.decode(project.getKeyFile(), project.getAccountFile());
			Session session = Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(account.getName(), account.getPass());
				}
			});

			// メール本文取得
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(project.getWorkDir() + "//mailtext.txt"), StandardCharsets.UTF_8));
			String text = reader.readLine() + "\r\n";
			String line = null;
			while ((line = reader.readLine()) != null) {
				text += line;
				text += "\r\n";
			}
			reader.close();

			// メッセージ作成
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(project.getFrom()));

			Multipart mp = new MimeMultipart();
			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setText(text, "UTF-8", "plain");
			mp.addBodyPart(mbp);

			MimeBodyPart mbpFile = new MimeBodyPart();
			FileDataSource fds = new FileDataSource(textFileName);
			mbpFile.setDataHandler(new DataHandler(fds));
			mbpFile.setFileName(MimeUtility.encodeWord(fds.getName()));
			mp.addBodyPart(mbpFile);

			msg.setSubject("Notification of Changed Clone Information");
			msg.setContent(mp);

			for (String to : project.getToList()) {
				sendmail(msg, to);
			}
		} catch (Exception e) {
			Logger.writeError(e);
			Logger.writeln("Can't send mail.", Logger.ERROR);
		}
	}

	/**
	 * <p>
	 * メール送信
	 * </p>
	 *
	 * @param msg メッセージ
	 * @param to  メール送信先
	 */
	private static void sendmail(MimeMessage msg, String to) {
		if (!to.equals("NULL")) {
			try {
				msg.setRecipients(Message.RecipientType.TO, to);
				Transport.send(msg);
				Logger.writeln("<Success> Send mail to " + to, Logger.SYSTEM);
			} catch (MessagingException e) {
				Logger.writeError(e);
				Logger.writeln("Can't send mail to " + to, Logger.ERROR);
			}
		}
	}
}
