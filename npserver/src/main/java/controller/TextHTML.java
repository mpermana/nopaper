package controller;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import spark.Request;
import spark.Response;
import spark.Spark;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class TextHTML {

	private final Configuration configuration;

	public TextHTML() {
		configuration = new Configuration();
		configuration
				.setClassForTemplateLoading(this.getClass(), "/freemarker");
	}

	private String decodeParams(Request request, String queryParam) {
		try {
			return URLDecoder.decode(request.params(queryParam), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public void addRoutes() {
		Spark.get(new spark.Route("/text/html/:topic") {
			@Override
			public Object handle(final Request request, final Response response) {
				response.header("Content-Type", "text/html");

				String topic = decodeParams(request, "topic");
				Template template;
				try {
					template = configuration.getTemplate("texthtml.ftl");
					StringWriter stringWriter = new StringWriter();
					Object dataModel = m.T.dict("topic", topic);
					template.process(dataModel, stringWriter);
					return stringWriter;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		});

	}
}
