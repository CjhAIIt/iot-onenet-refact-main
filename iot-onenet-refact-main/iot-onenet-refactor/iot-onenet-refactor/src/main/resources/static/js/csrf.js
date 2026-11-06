async function csrfHeaders() {
    const response = await fetch('/api/csrf', { credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new Error('Unable to obtain CSRF token');
    const csrf = await response.json();
    return { [csrf.headerName]: csrf.token };
}
