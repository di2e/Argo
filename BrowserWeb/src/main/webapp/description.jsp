<%@ page import="java.net.InetAddress" %>

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%

	// The full protocol, hostname, and port (eg, http://something.org:8080)
	String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
	StringBuffer requestURL = request.getRequestURL();
	
	String ctxPath = request.getContextPath();
	String basePath = hostIPAddr + ctxPath;
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Insert title here</title>
</head>
<body>

</body>
</html>