package de.jojii.matrixclientserver.Networking;

import de.jojii.matrixclientserver.Callbacks.DataCallback;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class HttpHelper {
    public static class URLs{
        private static String root = "_matrix/";
        public static String client = root+"client/r0/";
        public static String media = root+"media/r0/";

		public static String directory = client + "directory/";
        public static String login = client+"login";
        public static String logout = client+"logout";
        public static String logout_all = client+"logout/all";
        public static String whoami = client+"account/whoami";
        public static String presence = client+"presence/";
        public static String rooms = client+"rooms/";
        public static String sync = client+"sync";
        public static String user  = client+"user/";
        public static String upload = media+"upload/";
    }

	public HttpHelper(Supplier<String> accessTokenSupplier) {
		this.accessTokenSupplier = accessTokenSupplier;
	}

	private Supplier<String> accessTokenSupplier;

    public String sendRequest(String host, String path, JSONObject data, boolean useAccesstoken, String requestMethod) throws IOException {
		return sendRequest(host, path, data, useAccesstoken, requestMethod, false);
	}

	public String sendRequest(String host, String path, JSONObject data, boolean useAccesstoken, String requestMethod,
			boolean throwAll) throws IOException {

		URL obj = URI.create(host + path).toURL();
		URLConnection con = obj.openConnection();

		HttpURLConnection http = (HttpURLConnection) con;
		http.setRequestMethod(requestMethod);
		http.setDoOutput(true);
		http.setReadTimeout(60000);

		if (useAccesstoken) {
			http.setRequestProperty("Authorization", "Bearer " + accessTokenSupplier.get());
		}

		if (data != null) {
			int length = data.toString().length();
			http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		}

		http.connect();

		if (data != null) {
			try (OutputStream os = http.getOutputStream()) {
				os.write(data.toString().getBytes());
			}
		}

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			return response.toString();
		} catch (IOException e) {

			String errorResponse = null;
			InputStream errorStream = http.getErrorStream();
			if (errorStream != null) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream))) {
					StringBuilder response = new StringBuilder();
					String responseLine = null;
					while ((responseLine = br.readLine()) != null) {
						response.append(responseLine.trim());
					}
					errorResponse = response.toString();
				}
			}
			if (throwAll) {
				throw new IOException(errorResponse, e);
			}
			return "{\n" + "  \"response\":\"error\",\n" + "  \"code\":" + http.getResponseCode() + "\n" + "}";
		}
    }

    public String sendStream(String host, String path, String contentType, InputStream data, int contentLength, boolean useAccesstoken, String requestMethod) throws IOException {
		URL obj = URI.create(host + path).toURL();
        URLConnection con = obj.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod(requestMethod);
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", contentType);
        http.addRequestProperty("Content-Length", Integer.toString(contentLength));

		if (useAccesstoken) {
			http.setRequestProperty("Authorization", "Bearer " + accessTokenSupplier.get());
		}

        try(OutputStream os = http.getOutputStream()) {
            int i = 0;
            int bytes = 0;
            while(bytes != -1) {
                byte []buff = new byte[1024];
                bytes = data.read(buff);
                if (bytes != -1) {
                    os.write(buff, 0, bytes);
                    i += bytes;
                }
            }

            os.flush();
            os.close();
        }

        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }catch (IOException e){
            return "{\n" +
                    "  \"response\":\"error\",\n" +
                    "  \"code\":"+http.getResponseCode()+"\n" +
                    "}";
        }
    }

    public void sendStreamAsync(String host, String path, String contentType, int contentLength, InputStream data, boolean useAccesstoken, String requestMethod, DataCallback callback) throws IOException {
        if(callback == null){
            System.err.println("callback must not be null!");
            return;
        }
        new Thread(() -> {
            try {
                String res = sendStream(host,path,contentType,data, contentLength, useAccesstoken,requestMethod);
                callback.onData(res);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendRequestAsync(String host, String path, JSONObject data, DataCallback callback) throws IOException {
		sendRequestAsync(host, path, data, callback, accessTokenSupplier.get() != null, "POST");
    }

    public void sendRequestAsync(String host, String path, JSONObject data, String requestMethd, DataCallback callback) throws IOException {
		sendRequestAsync(host, path, data, callback, accessTokenSupplier.get() != null, requestMethd);
    }

    public void sendRequestAsync(String host, String path, JSONObject data, DataCallback callback, boolean useAccesstoken, String requestMethod) throws IOException {
        if(callback == null){
            System.err.println("callback must not be null!");
            return;
        }
        new Thread(() -> {
            try {
                String res = sendRequest(host,path,data,useAccesstoken,requestMethod);
                callback.onData(res);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
