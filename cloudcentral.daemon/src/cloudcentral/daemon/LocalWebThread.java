package cloudcentral.daemon;

import java.io.*;
import java.net.*;

public class LocalWebThread extends Thread {
	PipedInputStream[] filein;
	PipedReader[] reader;
	PipedWriter[] writer;
	String[] names;

	public LocalWebThread(PipedInputStream[] _filein, PipedReader[] _reader, PipedWriter[] _writer, String[] _names) {
		filein = _filein;
		reader = _reader;
		writer = _writer;
		names = _names;
	}

	public void run() {
		ServerSocket server = null;
		try {
			// ��Ŀ��˿ڽ���serversocket����������servlet����Ϣ
			server = new ServerSocket(43967, 0, InetAddress.getByName("127.0.0.1"));
			while (true) {
				Socket s = server.accept();
				System.out.println("LocalWebThread:connected");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				String command = br.readLine();
				System.out.println("LocalWebThread:received command=" + command);
				// ������ʸ�Ŀ¼����
				if (command.equals("<getclients>")) {
					for (String sendnames : names) {
						if (sendnames != null) {
							bw.write(sendnames + "\n");
							bw.flush();
						}
					}
					bw.write("<end>\n");
					bw.flush();
					// ����������������
				} else if (command.equals("<webaccess>")) {
					String clientname = br.readLine();
					String path = br.readLine();
					System.out.println("LocalWebThread:clientname=" + clientname);
					System.out.println("LocalWebThread:path=" + path);
					int i;
					// ����Ŀ��client��id
					for (i = 0; i < names.length; i++) {
						if (names[i] != null)
							if (names[i].equals(clientname))
								break;
					}
					// ������client���˳�
					if (i == names.length) {

						bw.write("<notfound>\n");
						bw.flush();
						System.out.println("LocalWebThread:client not found");
						bw.close();
						br.close();
						s.close();
						continue;
					}
					//ͨ��id�õ�ͨ�ŵĹܵ�
					PipedInputStream pis = filein[i];
					BufferedWriter pbw = new BufferedWriter(writer[i]);
					BufferedReader pbr = new BufferedReader(reader[i]);
					pbw.write(path + "\n");
					pbw.flush();

					String strbuf = new String();
					try {
						strbuf = pbr.readLine();
					} catch (IOException e) {
						System.out.println("LocalWebThread:"+e);
						bw.write("<notfound>\n");
						bw.flush();
						System.out.println("LocalWebThread:client not found");
						bw.close();
						br.close();
						s.close();
						names[i] = null;
						continue;
					}
					///System.out.println("LocalWebThread:pbr.readline()");
					// System.out.println("LocalWebThread:inBuff"+strbuf);
					// ��ȡ�ļ�����
					System.out.println("LocalWebThread:type=" + strbuf);
					bw.write(strbuf + "\n");
					bw.flush();
					// ����Ŀ¼
					if (strbuf.equals("<directory>")) {
						// System.out.println("LocalWebThread:open directory"+path);
						while (true) {

							strbuf = pbr.readLine();
							System.out.println("LocalWebThread: fileindir=" + strbuf);
							bw.write(strbuf + "\n");
							bw.flush();
							if (strbuf.equals("<enddir>"))
								break;
						}
					} else if (strbuf.equals("<notfound>")) {
						bw.write("<notfound>\n");
						bw.flush();
						System.out.println("LocalWebThread:file not found");
					} else {
						// ��ȡ�ļ�����
						System.out.println("LocalWebThread:read pipe... " + strbuf);
						strbuf = pbr.readLine();

						long filelen = Long.parseLong(strbuf);
						System.out.println("LocalWebThread:" + filelen);
						DataOutputStream dout = new DataOutputStream(s.getOutputStream());
						dout.writeLong(filelen);
						dout.flush();
						byte[] fBuff = new byte[1024];
						int length = 0;
						long receivedlength = 0;
						System.out.println("LocalWebThread:start transmit file " + strbuf);
						try {
							while (true) {
								// s.sendUrgentData(0xff);
								//�ļ����䣬ͨ����ȡ��Ϣ���״̬���������ȡ����servlet��ر�socket���ӣ���ȡ���������Ϊnull
								//�����ӹرգ�ֹͣ������������
								pbw.write("reading\n");
								pbw.flush();
								if (pis != null) {
									length = pis.read(fBuff);
									System.out.println("LocalWebThread:pis.read completed");
								}
								String servletresponse = br.readLine();
								System.out.println("LocalWebThread:" + servletresponse);
								if (servletresponse == null) {
									pbw.write("close\n");
									pbw.flush();
									break;
								}
								dout.write(fBuff, 0, length);
								dout.flush();

								System.out.println("LocalWebThread:transmitted " + length);
								receivedlength += length;
								if (receivedlength == filelen)
									break;
							}
							dout.close();
						} catch (IOException e) {
							System.out.println(e);
							if (dout != null)
								dout.close();
							if (s != null)
								s.close();
						}
						System.out.println("LocalWebThread:write completed");
					}

					bw.close();
					br.close();
					s.close();
				}
			}
		}

		catch (IOException e) {
			System.out.println("LocalWebThread:" + e);
			try {
				if (server != null)
					server.close();
			} catch (IOException e1) {
				System.out.println("LocalWebThread:" + e1);
			}

		}
	}

}
