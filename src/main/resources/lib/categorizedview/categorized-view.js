document.querySelectorAll("table.categorizedSortable").forEach(function (e) {
    e.sortable = new CategorizedSortable.CategorizedSortable(e);
});
