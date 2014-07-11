package org.jvnet.hudson.hadoop.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jvnet.hudson.hadoop.HDFSArchiver;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class BaseServlet extends HttpServlet{
	private Log log = LogFactory.getLog("hdfs.servlet");
	private static final long serialVersionUID = 1L;
	protected TemplateEngine templateEngine;
	
	@Override
    public void init(final ServletConfig config) throws ServletException {
		URI staticBase = null;
		try {
			staticBase = this.getClass().getClassLoader().getResource("org/jvnet/hudson/hadoop/servlet/static/").toURI();
		} catch (URISyntaxException e) {
			log.warn("Failed to locate templates root.");
		}
		templateEngine = new HotTemplateEngine(staticBase, null);
	}

	
	protected void outputStatic(String name, Writer out) throws IOException{
		String path = "org/jvnet/hudson/hadoop/servlet/static/" + name;
	    InputStream ins = this.getClass().getClassLoader().getResourceAsStream(path);
	    byte[] buffer = new byte[128 * 1024];
	    if(ins != null){
	    	int len = ins.read(buffer);
	    	String prefix = HDFSArchiver.getArchiver().prefix;
	    	out.write(new String(buffer, 0, len, "utf8").replace("${prefix}", prefix));
	    }
	}
	
	protected void renderTemplate(String name, Object context, Writer out) throws IOException{
		templateEngine.render(name, context, out);
	}
}
