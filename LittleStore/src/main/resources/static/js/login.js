function viewPassword() {
	var passwordInput = document.getElementById('password');
	var passStatus = document.getElementById('pass-status');
	
	if (passwordInput.type == 'password') {
		passwordInput.type='text';
		passStatus.className='fa fa-eye-slash';
	}
	else {
		passwordInput.type='password';
		passStatus.className='fa fa-eye';
	}
}