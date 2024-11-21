window.addEventListener("DOMContentLoaded", function() {
    console.log("DOMContentLoaded");
    document.querySelectorAll("table.categorizedSortable").forEach(function (e){
        e.sortable = new CategorizedSortable.CategorizedSortable(e);
    })
});
