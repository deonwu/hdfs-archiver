package org.jvnet.hudson.hadoop.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jvnet.hudson.hadoop.HDFSArchiver;

public class WelcomeIndex extends BaseServlet{
	private static final long serialVersionUID = 1L;

	protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException{
	    response.setContentType("text/html");
	    response.setCharacterEncoding("utf-8");
		response.getWriter().print("prefix:" + HDFSArchiver.getArchiver().prefix);
	} 
}
