<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="userName" value="${pageContext.request.userPrincipal.name}" />
<!-- 			<nav>
				<i class="fas fa-bars"></i>
				Menu
			</nav> -->
			<nav>
				<input type="text" placeholder=" Search">
			</nav>
 			<div id="action-icons">
				<form id="logoutForm" method="POST" action="${contextPath}/logout">
					<span>${userName != null ? 'Logged in as: '.concat(userName) : 'Login/Sign Up ->'}</span>
				<c:if test="${userName != null}">
	    	        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
					<input type="submit" id="logoutbtn" name="logoutbtn" value="Sign Out" class="btn btn-sm btn-primary btn-block" />
				</c:if>
					<a href="${pageContext.request.userPrincipal.name != null ? '/account' : '/login'}">
						<i class="fas fa-user-alt" title="${pageContext.request.userPrincipal.name != null ? 'Account' : 'Sign Up/Login'}"></i></a>
					<a href="/cart"><i class="fas fa-shopping-cart" title="Shopping Cart"></i></a>
				</form>				
			</div>