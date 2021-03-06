<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<!DOCTYPE html>
<html lang="en">
	<head>
		<jsp:include page="headElement.jsp">
			<jsp:param name="title" value="The Little Store - Home" />
			<jsp:param name="page" value="index" />
		</jsp:include>
	</head>
	<body>
		<header>
			<jsp:include page="basicHeader.jsp"></jsp:include>
		</header>
		<div id="main-content">
			<jsp:include page="sideNav.jsp"></jsp:include>
			<div id="center-content"><!--  style="height:100%; background-image:url('images/Main_BG.jpg');opacity:50%;"> -->
				<p>The Little Store</p>
				<img src="images/Main_BG.jpg" alt="Stockpile Photo" />
			</div>
		</div>
		<footer>
			<jsp:include page="basicFooter.jsp"></jsp:include>
		</footer>
	</body>
</html>