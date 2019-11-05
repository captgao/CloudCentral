package cloudcentral;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "FileExplorerServlet", urlPatterns = { "/files" })
public class FileExplorer extends HttpServlet {
	Socket socket;
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// 获取对应的请求参数
		String client = request.getParameter("client");
		String requestpath = request.getParameter("path");
		// 根据请求参数去调用对应的方法。
		if (client == null)
			default_page(request, response);
		else {
			if (requestpath == null)
				requestpath = "";
			clientexp(request, response, client, requestpath);
		}
		if (socket != null)
			socket.close();
	}

	private void default_page(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		BufferedReader br;
		BufferedWriter bw;
		socket = new Socket("localhost", 43967);
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		bw.write("<getclients>\n");
		bw.flush();
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		ArrayList<String> client_name = new ArrayList<String>();
		String namecache;
		while (!(namecache = br.readLine()).equals("<end>")) {
			client_name.add(namecache);
		}
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head>");
		out.println("<title>Cloud Central Index</title>");
		out.println("</head>");
		out.println("<body>");
		out.println("<h2>Hello " + "</h2>");
		out.println("<h2>The time right now is : " + new Date() + "</h2>");
		out.println("<h2>Clients:</h2>");
		// out.println(System.getProperty("user.dir"));//
		for (String namecache_print : client_name) {
			out.println("<a href=\"files?client=" + namecache_print + "\">" + namecache_print + "</a>");
			out.println("<br>");
		}
		out.println("</body>");
		out.println("</html>");
		socket.close();
		out.close();
	}

	public void clientexp(HttpServletRequest request, HttpServletResponse response, String client, String path)
			throws ServletException, IOException {
		BufferedReader br;
		BufferedWriter bw;
		socket = new Socket("localhost", 43967);
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		bw.write("<webaccess>\n");
		bw.flush();
		bw.write(client + "\n");
		bw.flush();
		bw.write(path + "\n");
		bw.flush();
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String filetype = br.readLine();
		// 404
		if (filetype.equals("<notfound>")) {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>");
			out.println("<title>404</title>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h2>Not Found</h2>");
			out.println("</body>");
			out.println("</html>");
			out.close();

		}
		// directory
		else if (filetype.equals("<directory>")) {

			ArrayList<String> filelist = new ArrayList<String>();
			String namecache = new String();
			while (true) {
				namecache = br.readLine();
				if (namecache.equals("<enddir>"))
					break;
				filelist.add(namecache);
			}
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>");
			out.println("<title>" + client + path + "</title>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h2>" + client + path + "</h2>");
			out.println("<h2>Files</h2>");
			for (String namecache_print : filelist) {
				out.println("<a href=\"files?client=" + client + "&path=/" + namecache_print + "\">" + namecache_print
						+ "</a>");
				out.println("<br>");
			}
			out.println("</body>");
			out.println("</html>");
			out.close();

		}
		// file
		else {
			try {
				// set MIME-type
				response.setContentType(filetype);
				// initial to received long filelen
				DataInputStream din = new DataInputStream(socket.getInputStream());
				long filelen = din.readLong();
				// set ContentLengthLong, supports large file
				response.setContentLengthLong(filelen);
				String[] names = path.split("/");
				String filename = names[names.length - 1];
				response.setHeader("Content-Disposition", "attachment;filename=" + filename);
				OutputStream httpout = response.getOutputStream();
				System.out.println("getOutputStream()");
				byte[] inputByte = new byte[1024];
				long length = 0, receivedlength = 0;
				while (true) {
					bw.write("received\n");
					bw.flush();
					if (din != null) {
						length = din.read(inputByte);
					}
					httpout.write(inputByte, 0, (int) length);
					httpout.flush();
					receivedlength += length;
					if (receivedlength == filelen) {
						break;
					}

				}
				httpout.close();
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				if (socket != null)
					socket.close();
			}

		}
		socket.close();

	}

	public void destroy(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (socket != null) {
			socket.close();
			System.out.println("socket closed");
		}
	}
}
