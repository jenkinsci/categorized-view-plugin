const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {test} = require('node:test');

const source = fs.readFileSync(
    path.join(__dirname, '..', '..', 'main', 'webapp', 'catSortable.js'), 'utf8');

function loadScript(document, sessionStorage) {
  const factory = new Function('document', 'sessionStorage', source + '\nreturn CategorizedSortable;');
  return factory(document, sessionStorage).CategorizedSortable;
}

function makeHeaderCell(label, initialSortDir) {
  const link = {lastChild: {}, parentNode: null, onclick: null};
  const cell = {
    innerText: label,
    innerHTML: '',
    cellIndex: 0,
    firstChild: link,
    getAttribute: (name) => (name === 'initialSortDir' ? initialSortDir : null),
  };
  link.parentNode = cell;
  return cell;
}

function makeJobRow(id, name, attributes = {}) {
  return {
    id,
    cells: [{getAttribute: (attr) => (attr === 'data' ? name : null)}],
    classList: {contains: () => false},
    getAttribute: (attr) => (attributes[attr] ?? null),
  };
}

function makeView(headerCells, jobRows) {
  const appended = [];
  const table = {
    rows: [{cells: headerCells}, ...jobRows],
    tBodies: [{appendChild: (row) => appended.push(row)}],
  };
  const document = {
    location: 'http://localhost/jenkins/view/MyView/',
    querySelectorAll: () => [table],
  };
  const sessionStorage = {
    store: new Map(),
    getItem(key) {
      return this.store.has(key) ? this.store.get(key) : null;
    },
    setItem(key, value) {
      this.store.set(key, value);
    },
  };
  return {table, appended, document, sessionStorage};
}

function displayedNames(view) {
  const order = view.appended.length ? view.appended : view.table.rows.slice(1);
  return order.map((row) => row.cells[0].getAttribute('data'));
}

const SERVER_ORDER = ['m-job', 'a-job', 'z-job'];

function makeDefaultView() {
  return makeView([makeHeaderCell('Name', 'down'), makeHeaderCell('Last Success')], [
    makeJobRow('job_m', 'm-job'),
    makeJobRow('job_a', 'a-job'),
    makeJobRow('job_z', 'z-job'),
  ]);
}

test('initialSortDir on a header does not reorder rows on load', () => {
  // The standard JobColumn's initialSortDir must not override grouping-rule order.
  const view = makeDefaultView();
  const Sortable = loadScript(view.document, view.sessionStorage);

  new Sortable(view.table);

  assert.deepEqual(displayedNames(view), SERVER_ORDER);
});

test('a stored sort preference still reorders rows on load', () => {
  const view = makeDefaultView();
  view.sessionStorage.setItem('catts_direction::http://localhost/jenkins/view/MyView/::0', '0:down');
  const Sortable = loadScript(view.document, view.sessionStorage);

  new Sortable(view.table);

  assert.deepEqual(displayedNames(view), ['a-job', 'm-job', 'z-job']);
});

test('clicking a header sorts the rows and stores the preference', () => {
  const view = makeDefaultView();
  const Sortable = loadScript(view.document, view.sessionStorage);
  new Sortable(view.table);
  view.appended.length = 0;

  view.table.rows[0].cells[0].firstChild.onclick();

  assert.deepEqual(displayedNames(view), ['z-job', 'm-job', 'a-job']);
  assert.equal(
      view.sessionStorage.getItem('catts_direction::http://localhost/jenkins/view/MyView/::0'),
      '0:up');
});
