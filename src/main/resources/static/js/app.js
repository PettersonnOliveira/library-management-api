(function () {
  'use strict';

  const VIEW_META = {
    dashboard: { title: 'Visão geral', subtitle: 'Resumo do acervo e dos empréstimos' },
    books: { title: 'Livros', subtitle: 'Acervo da biblioteca' },
    loans: { title: 'Empréstimos', subtitle: 'Empréstimos ativos e histórico' },
    users: { title: 'Usuários', subtitle: 'Usuários cadastrados' }
  };

  const state = {
    session: null,
    view: 'dashboard',
    books: [],
    loans: [],
    users: [],
    filters: { bookSearch: '', loanStatus: 'all' },
    loading: false
  };

  let modalCancelHandler = null;

  const $ = (selector, root) => (root || document).querySelector(selector);
  const $$ = (selector, root) => Array.from((root || document).querySelectorAll(selector));
  const viewRoot = () => $('#view-root');

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function isAdmin() {
    return !!state.session && state.session.user.role === 'ADMIN';
  }

  function initials(name) {
    const parts = String(name || '?').trim().split(/\s+/).filter(Boolean);
    const first = parts[0] ? parts[0][0] : '?';
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase();
  }

  function formatDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (isNaN(date.getTime())) return esc(value);
    return date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
  }

  /* ---------- Boot ---------- */
  document.addEventListener('DOMContentLoaded', init);

  function init() {
    bindAuthEvents();
    bindAppEvents();
    const session = Session.load();
    if (session && session.token) {
      state.session = session;
      showApp();
    } else {
      showAuth();
    }
  }

  /* ---------- Authentication ---------- */
  function bindAuthEvents() {
    $$('[data-auth-tab]').forEach(tab => {
      tab.addEventListener('click', () => {
        $$('[data-auth-tab]').forEach(item => item.classList.toggle('is-active', item === tab));
        const target = tab.dataset.authTab;
        $('#login-form').classList.toggle('is-hidden', target !== 'login');
        $('#register-form').classList.toggle('is-hidden', target !== 'register');
        const first = target === 'login' ? $('#login-email') : $('#register-name');
        if (first) first.focus();
      });
    });

    $('#login-form').addEventListener('submit', handleLogin);
    $('#register-form').addEventListener('submit', handleRegister);
  }

  async function handleLogin(event) {
    event.preventDefault();
    const button = event.target.querySelector('button[type="submit"]');
    const email = $('#login-email').value.trim();
    const password = $('#login-password').value;

    if (!email || !password) {
      toast('Informe e-mail e senha.', 'error');
      return;
    }

    setBusy(button, true, 'Entrando...');
    try {
      const data = await Api.request('/auth/login', {
        method: 'POST',
        body: { email, password },
        auth: false
      });
      startSession(data);
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      setBusy(button, false);
    }
  }

  async function handleRegister(event) {
    event.preventDefault();
    const button = event.target.querySelector('button[type="submit"]');
    const name = $('#register-name').value.trim();
    const email = $('#register-email').value.trim();
    const password = $('#register-password').value;
    const confirmation = $('#register-confirm').value;

    if (!name || !email || !password) {
      toast('Preencha todos os campos.', 'error');
      return;
    }
    if (password.length < 8) {
      toast('A senha precisa ter pelo menos 8 caracteres.', 'error');
      return;
    }
    if (password !== confirmation) {
      toast('As senhas não conferem.', 'error');
      return;
    }

    setBusy(button, true, 'Criando conta...');
    try {
      await Api.request('/auth/register', {
        method: 'POST',
        body: { name, email, password },
        auth: false
      });
      const data = await Api.request('/auth/login', {
        method: 'POST',
        body: { email, password },
        auth: false
      });
      toast('Conta criada com sucesso!', 'success');
      startSession(data);
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      setBusy(button, false);
    }
  }

  function startSession(data) {
    state.session = {
      token: data.token,
      user: {
        id: data.userId,
        name: data.name,
        email: data.email,
        role: data.role
      }
    };
    Session.save(state.session);
    $('#login-form').reset();
    showApp();
  }

  function showAuth() {
    $('#app-screen').classList.add('is-hidden');
    $('#auth-screen').classList.remove('is-hidden');
    closeModal({ cancelled: false });
    const loginEmail = $('#login-email');
    if (loginEmail) loginEmail.focus();
  }

  function showApp() {
    $('#auth-screen').classList.add('is-hidden');
    $('#app-screen').classList.remove('is-hidden');
    renderSession();
    state.loading = true;
    navigate(routeFromHash(), false);
    loadAll();
  }

  function renderSession() {
    const user = state.session.user;
    $('#user-name').textContent = user.name;
    $('#user-avatar').textContent = initials(user.name);
    $('#user-role').textContent = user.role;
    $('#topbar-role').textContent = user.role === 'ADMIN' ? 'Administrador' : 'Usuário';
    $$('.is-admin-only').forEach(element => element.classList.toggle('is-hidden', !isAdmin()));
  }

  function handleLogout(notify) {
    Session.clear();
    state.session = null;
    state.books = [];
    state.loans = [];
    state.users = [];
    state.view = 'dashboard';
    history.replaceState(null, '', location.pathname);
    showAuth();
    if (notify) toast('Você saiu da conta.', 'info');
  }

  function setBusy(button, busy, label) {
    if (!button) return;
    if (busy) {
      button.dataset.label = button.textContent;
      button.textContent = label || 'Aguarde...';
      button.disabled = true;
    } else {
      button.textContent = button.dataset.label || button.textContent;
      button.disabled = false;
    }
  }

  /* ---------- Navigation ---------- */
  function routeFromHash() {
    const route = location.hash.replace(/^#\/?/, '');
    return VIEW_META[route] ? route : 'dashboard';
  }

  function navigate(view, updateHash) {
    if (!VIEW_META[view]) view = 'dashboard';
    if (view === 'users' && !isAdmin()) view = 'dashboard';
    state.view = view;

    if (updateHash !== false) {
      const target = '#/' + view;
      if (location.hash !== target) location.hash = target;
    }

    const meta = VIEW_META[view];
    $('#view-title').textContent = meta.title;
    $('#view-subtitle').textContent = meta.subtitle;
    $$('.nav-item').forEach(item => item.classList.toggle('is-active', item.dataset.view === view));
    $('#app-screen').classList.remove('nav-open');
    renderView();
  }

  function bindAppEvents() {
    $$('.nav-item').forEach(item => item.addEventListener('click', () => navigate(item.dataset.view)));
    $('#logout-btn').addEventListener('click', () => handleLogout(true));
    $('#menu-toggle').addEventListener('click', () => $('#app-screen').classList.toggle('nav-open'));
    $('#scrim').addEventListener('click', () => $('#app-screen').classList.remove('nav-open'));

    window.addEventListener('hashchange', () => {
      if (!state.session) return;
      const route = routeFromHash();
      if (route !== state.view) navigate(route, false);
    });

    viewRoot().addEventListener('click', handleViewClick);
  }

  async function loadAll() {
    if (!state.session) return;
    state.loading = true;
    renderView();

    try {
      const requests = [Api.get('/books'), Api.get('/loans')];
      if (isAdmin()) requests.push(Api.get('/users'));
      const [books, loans, users] = await Promise.all(requests);
      state.books = books || [];
      state.loans = loans || [];
      state.users = users || [];
      state.loading = false;
      renderView();
    } catch (error) {
      state.loading = false;
      if (error.status === 401) {
        handleLogout(false);
        toast(error.message, 'error');
      } else {
        toast(error.message, 'error');
        renderView();
      }
    }
  }

  function renderView() {
    if (!state.session) return;
    if (state.loading) {
      viewRoot().innerHTML = '<div class="loading"><span class="spinner"></span>Carregando dados...</div>';
      return;
    }
    switch (state.view) {
      case 'books': renderBooks(); break;
      case 'loans': renderLoans(); break;
      case 'users': renderUsers(); break;
      default: renderDashboard();
    }
  }
  /* ---------- Dashboard ---------- */
  function renderDashboard() {
    const totalBooks = state.books.length;
    const available = state.books.filter(book => book.available).length;
    const activeLoans = state.loans.filter(loan => !loan.returnDate).length;
    const returned = state.loans.length - activeLoans;
    const recent = state.loans
      .slice()
      .sort((a, b) => String(b.loanDate).localeCompare(String(a.loanDate)))
      .slice(0, 5);
    const admin = isAdmin();

    viewRoot().innerHTML = `
      <div class="stats">
        ${statCard('📚', 'Livros no acervo', totalBooks, 'books')}
        ${statCard('✅', 'Disponíveis', available, 'books')}
        ${statCard('🔁', 'Empréstimos ativos', activeLoans, 'loans')}
        ${statCard('📦', 'Devolvidos', returned, 'loans')}
      </div>
      <div class="grid-2">
        <div class="card">
          <div class="card-head">
            <h2>Empréstimos recentes</h2>
            <button class="btn btn-sm btn-ghost" data-action="go" data-view="loans">Ver todos</button>
          </div>
          ${recent.length ? `
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr><th>Livro</th>${admin ? '<th>Usuário</th>' : ''}<th>Data</th><th>Status</th></tr>
                </thead>
                <tbody>
                  ${recent.map(loan => `
                    <tr>
                      <td><div class="cell-main"><strong>${esc(loan.book.title)}</strong><small>${esc(loan.book.author)}</small></div></td>
                      ${admin ? `<td>${esc(loan.user.name)}</td>` : ''}
                      <td class="muted">${formatDate(loan.loanDate)}</td>
                      <td>${loan.returnDate
                        ? '<span class="badge badge-neutral">Devolvido</span>'
                        : '<span class="badge badge-warning">Em andamento</span>'}</td>
                    </tr>`).join('')}
                </tbody>
              </table>
            </div>`
            : emptyState('🔁', 'Nenhum empréstimo registrado', 'Os empréstimos aparecerão aqui.')}
        </div>
        <div class="card">
          <div class="card-head"><h2>Ações rápidas</h2></div>
          <div class="quick-actions">
            <button class="quick" data-action="new-loan"><span>🔁</span><span>Novo empréstimo</span></button>
            ${admin ? '<button class="quick" data-action="new-book"><span>📚</span><span>Cadastrar livro</span></button>' : ''}
            ${admin ? '<button class="quick" data-action="new-user"><span>👥</span><span>Cadastrar usuário</span></button>' : ''}
            <button class="quick" data-action="go" data-view="books"><span>📖</span><span>Explorar acervo</span></button>
          </div>
        </div>
      </div>`;
  }

  function statCard(icon, label, value, view) {
    return `<button class="stat" data-action="go" data-view="${view}">
      <span class="stat-top"><span class="stat-ico">${icon}</span><span class="badge badge-neutral">ver</span></span>
      <span class="stat-value">${value}</span>
      <span class="stat-label">${label}</span>
    </button>`;
  }

  function emptyState(icon, title, text) {
    return `<div class="empty"><span class="empty-ico">${icon}</span><strong>${esc(title)}</strong><span>${esc(text)}</span></div>`;
  }

  /* ---------- Books ---------- */
  function renderBooks() {
    viewRoot().innerHTML = `
      <div class="toolbar">
        <div class="search">
          <span>🔍</span>
          <input id="book-search" type="search" placeholder="Buscar por título ou autor..." value="${esc(state.filters.bookSearch)}">
        </div>
        ${isAdmin() ? '<button class="btn btn-primary" data-action="new-book">+ Novo livro</button>' : ''}
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Título</th><th>Autor</th><th>Status</th><th class="ta-right">Ações</th></tr></thead>
            <tbody id="books-tbody"></tbody>
          </table>
        </div>
      </div>`;

    const search = $('#book-search');
    if (search) {
      search.addEventListener('input', () => {
        state.filters.bookSearch = search.value;
        updateBookRows();
      });
    }
    updateBookRows();
  }

  function filteredBooks() {
    const query = state.filters.bookSearch.trim().toLowerCase();
    if (!query) return state.books;
    return state.books.filter(book =>
      String(book.title).toLowerCase().includes(query) ||
      String(book.author).toLowerCase().includes(query));
  }

  function updateBookRows() {
    const tbody = $('#books-tbody');
    if (!tbody) return;
    const books = filteredBooks();

    if (!books.length) {
      const message = state.books.length
        ? 'Tente outro termo de busca.'
        : 'Cadastre o primeiro livro do acervo.';
      tbody.innerHTML = `<tr><td colspan="4">${emptyState('📚', 'Nenhum livro encontrado', message)}</td></tr>`;
      return;
    }

    tbody.innerHTML = books.map(book => `
      <tr>
        <td><strong>${esc(book.title)}</strong></td>
        <td class="muted">${esc(book.author)}</td>
        <td>${book.available
          ? '<span class="badge badge-success">Disponível</span>'
          : '<span class="badge badge-warning">Emprestado</span>'}</td>
        <td>
          <div class="actions">
            ${book.available ? `<button class="btn btn-sm" data-action="new-loan" data-book-id="${book.id}">Emprestar</button>` : ''}
            ${isAdmin() ? `<button class="btn btn-sm btn-ghost" data-action="edit-book" data-id="${book.id}">Editar</button>` : ''}
          </div>
        </td>
      </tr>`).join('');
  }

  /* ---------- Loans ---------- */
  function renderLoans() {
    const admin = isAdmin();
    const statuses = [['all', 'Todos'], ['active', 'Ativos'], ['returned', 'Devolvidos']];

    viewRoot().innerHTML = `
      <div class="toolbar">
        <div class="segmented" id="loan-filters">
          ${statuses.map(([value, label]) =>
            `<button data-status="${value}" class="${state.filters.loanStatus === value ? 'is-active' : ''}">${label}</button>`
          ).join('')}
        </div>
        <button class="btn btn-primary" data-action="new-loan">+ Novo empréstimo</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                ${admin ? '<th>Usuário</th>' : ''}
                <th>Livro</th><th>Empréstimo</th><th>Devolução</th><th>Status</th><th class="ta-right">Ações</th>
              </tr>
            </thead>
            <tbody id="loans-tbody"></tbody>
          </table>
        </div>
      </div>`;

    $$('#loan-filters button').forEach(button => {
      button.addEventListener('click', () => {
        state.filters.loanStatus = button.dataset.status;
        $$('#loan-filters button').forEach(item => item.classList.toggle('is-active', item === button));
        updateLoanRows();
      });
    });
    updateLoanRows();
  }

  function filteredLoans() {
    const status = state.filters.loanStatus;
    return state.loans
      .filter(loan => {
        if (status === 'active') return !loan.returnDate;
        if (status === 'returned') return !!loan.returnDate;
        return true;
      })
      .sort((a, b) => String(b.loanDate).localeCompare(String(a.loanDate)));
  }

  function updateLoanRows() {
    const tbody = $('#loans-tbody');
    if (!tbody) return;
    const admin = isAdmin();
    const loans = filteredLoans();
    const columns = admin ? 6 : 5;

    if (!loans.length) {
      tbody.innerHTML = `<tr><td colspan="${columns}">${emptyState('🔁', 'Nenhum empréstimo', 'Registre um empréstimo para começar.')}</td></tr>`;
      return;
    }

    tbody.innerHTML = loans.map(loan => `
      <tr>
        ${admin ? `<td><div class="cell-main"><strong>${esc(loan.user.name)}</strong><small>${esc(loan.user.email)}</small></div></td>` : ''}
        <td><div class="cell-main"><strong>${esc(loan.book.title)}</strong><small>${esc(loan.book.author)}</small></div></td>
        <td class="muted">${formatDate(loan.loanDate)}</td>
        <td class="muted">${loan.returnDate ? formatDate(loan.returnDate) : '—'}</td>
        <td>${loan.returnDate
          ? '<span class="badge badge-neutral">Devolvido</span>'
          : '<span class="badge badge-warning">Em andamento</span>'}</td>
        <td>
          <div class="actions">
            ${loan.returnDate ? '' : `<button class="btn btn-sm" data-action="return-loan" data-id="${loan.id}">Devolver</button>`}
          </div>
        </td>
      </tr>`).join('');
  }

  /* ---------- Users ---------- */
  function renderUsers() {
    viewRoot().innerHTML = `
      <div class="toolbar">
        <div class="search"><span>👥</span><span class="muted">${state.users.length} usuário(s) cadastrado(s)</span></div>
        <button class="btn btn-primary" data-action="new-user">+ Novo usuário</button>
      </div>
      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Nome</th><th>E-mail</th><th>ID</th></tr></thead>
            <tbody>
              ${state.users.length
                ? state.users.map(user => `
                  <tr>
                    <td><strong>${esc(user.name)}</strong></td>
                    <td class="muted">${esc(user.email)}</td>
                    <td class="muted">#${user.id}</td>
                  </tr>`).join('')
                : `<tr><td colspan="3">${emptyState('👥', 'Nenhum usuário', 'Cadastre o primeiro usuário.')}</td></tr>`}
            </tbody>
          </table>
        </div>
      </div>`;
  }
  /* ---------- Actions ---------- */
  function handleViewClick(event) {
    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    switch (trigger.dataset.action) {
      case 'go':
        navigate(trigger.dataset.view);
        break;
      case 'new-book':
        openBookModal();
        break;
      case 'edit-book':
        openBookModal(Number(trigger.dataset.id));
        break;
      case 'new-loan':
        openLoanModal(trigger.dataset.bookId ? Number(trigger.dataset.bookId) : null);
        break;
      case 'return-loan':
        askReturnLoan(Number(trigger.dataset.id));
        break;
      case 'new-user':
        openUserModal();
        break;
      default:
        break;
    }
  }

  async function askReturnLoan(id) {
    const loan = state.loans.find(item => item.id === id);
    const title = loan ? loan.book.title : 'este empréstimo';
    const confirmed = await confirmDialog({
      title: 'Registrar devolução',
      message: `Confirmar a devolução de "${title}"? O livro voltará a ficar disponível.`,
      confirmLabel: 'Devolver'
    });
    if (!confirmed) return;

    try {
      await Api.put('/loans/' + id + '/return');
      toast('Devolução registrada com sucesso!', 'success');
      await loadAll();
    } catch (error) {
      toast(error.message, 'error');
    }
  }

  /* ---------- Book form ---------- */
  function openBookModal(bookId) {
    const book = bookId ? state.books.find(item => item.id === bookId) : null;

    openModal({
      title: book ? 'Editar livro' : 'Novo livro',
      submitLabel: book ? 'Salvar alterações' : 'Cadastrar livro',
      body: `
        <label class="field">
          <span>Título</span>
          <input id="book-title" type="text" placeholder="Ex.: Dom Casmurro" value="${book ? esc(book.title) : ''}" required>
        </label>
        <label class="field">
          <span>Autor</span>
          <input id="book-author" type="text" placeholder="Ex.: Machado de Assis" value="${book ? esc(book.author) : ''}" required>
        </label>
        ${book ? `
        <label class="field field-inline">
          <input id="book-available" type="checkbox" ${book.available ? 'checked' : ''}>
          <span>Disponível para empréstimo</span>
        </label>` : ''}`,
      onSubmit: async form => {
        const title = $('#book-title', form).value.trim();
        const author = $('#book-author', form).value.trim();
        if (!title || !author) throw new Error('Informe título e autor.');

        if (book) {
          await Api.put('/books/' + book.id, {
            title,
            author,
            available: $('#book-available', form).checked
          });
          toast('Livro atualizado!', 'success');
        } else {
          await Api.post('/books', { title, author });
          toast('Livro cadastrado!', 'success');
        }
        closeModal({ cancelled: false });
        await loadAll();
      }
    });
  }

  /* ---------- User form ---------- */
  function openUserModal() {
    openModal({
      title: 'Novo usuário',
      submitLabel: 'Cadastrar usuário',
      body: `
        <label class="field"><span>Nome</span><input id="new-user-name" type="text" placeholder="Nome completo" required></label>
        <label class="field"><span>E-mail</span><input id="new-user-email" type="email" placeholder="usuario@email.com" required></label>
        <label class="field"><span>Senha</span><input id="new-user-password" type="password" placeholder="Mínimo de 8 caracteres" minlength="8" required></label>
        <p class="muted" style="font-size:.8rem">Usuários criados por aqui recebem o perfil USER.</p>`,
      onSubmit: async form => {
        const name = $('#new-user-name', form).value.trim();
        const email = $('#new-user-email', form).value.trim();
        const password = $('#new-user-password', form).value;
        if (!name || !email || !password) throw new Error('Preencha todos os campos.');
        if (password.length < 8) throw new Error('A senha precisa ter pelo menos 8 caracteres.');

        await Api.post('/users', { name, email, password });
        closeModal({ cancelled: false });
        toast('Usuário cadastrado!', 'success');
        await loadAll();
      }
    });
  }

  /* ---------- Loan form ---------- */
  function openLoanModal(presetBookId) {
    const admin = isAdmin();
    const availableBooks = state.books.filter(book => book.available);

    if (!availableBooks.length) {
      toast('Nenhum livro disponível para empréstimo.', 'error');
      return;
    }
    if (admin && !state.users.length) {
      toast('Nenhum usuário cadastrado para receber o empréstimo.', 'error');
      return;
    }

    openModal({
      title: 'Novo empréstimo',
      submitLabel: 'Registrar empréstimo',
      body: `
        ${admin ? `
        <label class="field">
          <span>Usuário</span>
          <select id="loan-user" required>
            <option value="">Selecione o usuário...</option>
            ${state.users.map(user => `<option value="${user.id}">${esc(user.name)} — ${esc(user.email)}</option>`).join('')}
          </select>
        </label>`
        : '<p class="modal-text">O empréstimo será registrado no seu nome.</p>'}
        <label class="field">
          <span>Livro</span>
          <select id="loan-book" required>
            <option value="">Selecione o livro...</option>
            ${availableBooks.map(book => `<option value="${book.id}" ${String(book.id) === String(presetBookId) ? 'selected' : ''}>${esc(book.title)} — ${esc(book.author)}</option>`).join('')}
          </select>
        </label>`,
      onSubmit: async form => {
        const bookId = Number($('#loan-book', form).value);
        if (!bookId) throw new Error('Selecione um livro.');

        const body = { bookId };
        if (admin) {
          const userId = Number($('#loan-user', form).value);
          if (!userId) throw new Error('Selecione um usuário.');
          body.userId = userId;
        }

        await Api.post('/loans', body);
        closeModal({ cancelled: false });
        toast('Empréstimo registrado!', 'success');
        await loadAll();
      }
    });
  }

  /* ---------- Modal ---------- */
  function openModal(options) {
    const root = $('#modal-root');
    modalCancelHandler = options.onCancel || null;

    root.innerHTML = `
      <div class="modal-overlay" id="modal-overlay">
        <div class="modal" role="dialog" aria-modal="true" aria-label="${esc(options.title)}">
          <div class="modal-head">
            <h2>${esc(options.title)}</h2>
            <button type="button" class="icon-btn" data-close aria-label="Fechar">✕</button>
          </div>
          <form class="modal-body" id="modal-form" novalidate>
            ${options.body || ''}
            <p class="form-error is-hidden" id="modal-error"></p>
            <div class="modal-actions">
              <button type="button" class="btn btn-ghost" data-close>Cancelar</button>
              <button type="submit" class="btn btn-primary" id="modal-submit">${esc(options.submitLabel || 'Salvar')}</button>
            </div>
          </form>
        </div>
      </div>`;

    const overlay = $('#modal-overlay');
    const form = $('#modal-form');
    const submit = $('#modal-submit');
    const label = options.submitLabel || 'Salvar';

    overlay.addEventListener('click', event => {
      if (event.target === overlay) closeModal();
    });

    $$('[data-close]', root).forEach(element => {
      element.addEventListener('click', () => closeModal());
    });

    form.addEventListener('submit', async event => {
      event.preventDefault();
      hideModalError();
      submit.disabled = true;
      submit.textContent = 'Salvando...';
      try {
        await options.onSubmit(form);
      } catch (error) {
        showModalError(error.message || 'Não foi possível salvar.');
      } finally {
        submit.disabled = false;
        submit.textContent = label;
      }
    });

    document.addEventListener('keydown', onModalKeydown);
    const firstField = form.querySelector('input, select');
    if (firstField) firstField.focus();
  }

  function onModalKeydown(event) {
    if (event.key === 'Escape') closeModal();
  }

  function closeModal(options) {
    const root = $('#modal-root');
    if (!root.innerHTML) return;

    root.innerHTML = '';
    document.removeEventListener('keydown', onModalKeydown);

    const handler = modalCancelHandler;
    modalCancelHandler = null;
    if ((!options || options.cancelled !== false) && handler) handler();
  }

  function showModalError(message) {
    const box = $('#modal-error');
    if (!box) return;
    box.textContent = message;
    box.classList.remove('is-hidden');
  }

  function hideModalError() {
    const box = $('#modal-error');
    if (!box) return;
    box.textContent = '';
    box.classList.add('is-hidden');
  }

  function confirmDialog(options) {
    return new Promise(resolve => {
      openModal({
        title: options.title,
        submitLabel: options.confirmLabel || 'Confirmar',
        body: `<p class="modal-text">${esc(options.message)}</p>`,
        onCancel: () => resolve(false),
        onSubmit: async () => {
          closeModal({ cancelled: false });
          resolve(true);
        }
      });
    });
  }

  /* ---------- Toast ---------- */
  function toast(message, type) {
    const root = $('#toast-root');
    const element = document.createElement('div');
    element.className = 'toast toast-' + (type || 'info');
    element.textContent = message;
    root.appendChild(element);

    setTimeout(() => {
      element.classList.add('is-leaving');
      setTimeout(() => element.remove(), 260);
    }, 3600);
  }

})();