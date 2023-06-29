/*
The MIT Licence, for code from kryogenix.org

Code downloaded from the Browser Experiments section
of kryogenix.org is licenced under the so-called MIT
licence. The licence is below.

Copyright (c) 1997-date Stuart Langridge

Permission is hereby granted, free of charge, to any
person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the
Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
/*
Usage
=====

Add the "sortable" CSS class to a table to make it sortable.
The first column must be always table header, and the rest must be table data.
(the script seems to support rows to be fixed at the bottom, but haven't figured out how to use it.)

If the table data is sorted to begin with, you can add 'initialSortDir="up|down"' to the
corresponding column in the header row to display the direction icon from the beginning.
This is recommended to provide a visual cue that the table can be sorted.

The script guesses the table data, and try to use the right sorting algorithm.
But you can override this behavior by having 'data="..."' attribute on each row,
in which case the sort will be done on that field.
*/

var CategorizedSortable = (function () {


    function CategorizedSortable(table) {
        this.table = table;
        this.arrows = [];

        var firstRow = this.getFirstRow();
        if (!firstRow) return;

        // We have a first row: assume it's the header, and make its contents clickable links
        firstRow.forEach(function (cell) {
            cell.innerHTML = '<a href="#" class="sortheader">' + this.getInnerText(cell) + '<span class="sortarrow" /></a>';
            this.arrows.push(cell.firstChild.lastChild);

            var self = this;
            cell.firstChild.onclick = function () {
                self.onClicked(this);
                return false;
            };
        }.bind(this));

        // figure out the initial sort preference
        this.pref = this.getStoredPreference();
        if (this.pref == null) {
            firstRow.forEach(function (cell, i) {
                var initialSortDir = cell.getAttribute("initialSortDir");
                if (initialSortDir != null) {
                    this.pref = {
                        column: i,
                        direction: arrowTable[initialSortDir]
                    };
                }
            }.bind(this));
        }

        this.refresh();
    }

    CategorizedSortable.prototype = {
        /**
         * SPAN tags that we use to render directional arrows, for each columns.
         */
        arrows: null /*Array*/ ,

        /**
         * Current sort preference.
         */
        pref: null /* { column:int, direction:arrow } */ ,

        getFirstRow: function () {
            if (this.table.rows && this.table.rows.length > 0) {
                return Array.from(this.table.rows[0].cells);
            }
            return null;
        },

        getDataRows: function () {
            var newRows = [];
            var rows = this.table.rows;
            for (var j = 1; j < rows.length; j++) {
                newRows.push(rows[j]);
            }
            return newRows;
        },

        /**
         * If there's a persisted sort direction setting, retrieve it
         */
        getStoredPreference: function () {
            var key = this.getStorageKey();
            if (storage.hasKey(key)) {
                var val = storage.getItem(key);
                if (val) {
                    var vals = val.split(":");
                    if (vals.length == 2) {
                        return {
                            column: parseInt(vals[0]),
                            direction: arrowTable[vals[1]]
                        };
                    }
                }
            }
            return null;
        },


        getStorageKey: function () {
            var uri = document.location;
            var tableIndex = this.getIndexOfSortableTable();
            return "catts_direction::" + uri + "::" + tableIndex;
        },

        savePreference: function () {
            var key = this.getStorageKey();
            storage.setItem(key, this.pref.column + ":" + this.pref.direction.id);
        },

        /**
         * Determine the sort function for the specified column
         */
        getSorter: function (column) {
            var rows = this.table.rows;
            if (rows.length <= 1) return sorter.fallback;

            var itm = this.extractData(rows[1].cells[column]).trim();
            return sorter.determine(itm);
        },

        /**
         * Called when the column header gets clicked.
         */
        onClicked: function (lnk) {
            var arrow = lnk.lastChild;
            var th = lnk.parentNode;

            var column = th.cellIndex;
            if (column == (this.pref || {}).column) {
                // direction change on the same row
                this.pref.direction = this.pref.direction.next;
            } else {
                this.pref = {
                    column: column,
                    direction: arrow.sortdir || arrowTable.up
                };
            }

            arrow.sortdir = this.pref.direction; // remember the last sort direction on this column

            this.refresh();
            this.savePreference();
        },

        /**
         * Call when data has changed. Reapply the current sort setting to the existing data rows.
         * @since 1.484
         */
        refresh: function () {
            if (this.pref == null) return; // not sorting

            var column = this.pref.column;
            var dir = this.pref.direction;

            var s = this.getSorter(column);
            if (dir === arrowTable.up) { // ascending
                s = sorter.reverse(s);
            }

            // we allow some rows to stick to the top and bottom, so that is our first sort criteria
            // regardless of the sort function
            function rowPos(r) {
                if (r.classList.contains("sorttop")) return 0;
                if (r.classList.contains("sortbottom")) return 2;
                return 1;
            }

            var rows = this.getDataRows();
            rows.sort(function (a, b) {
                var x = rowPos(a) - rowPos(b);
                if (x != 0) return x;

                var aCell = this.getCell(a, column);
                var bCell = this.getCell(b, column);

                var aCategory = a.getAttribute("category");
                var bCategory = b.getAttribute("category");
                if (aCategory != null && aCategory == bCategory) {
                    if (a.getAttribute("categoryRole") == "category")
                        return -1;
                    else
                        return 1;
                }

                return s(this.extractData(aCell),
                    this.extractData(bCell));
            }.bind(this));

            rows.forEach(function (e) {
                this.table.tBodies[0].appendChild(e);
            }.bind(this));

            // update arrow rendering
            this.arrows.forEach(function (e, i) {
                e.innerHTML = ((i == column) ? dir : arrowTable.none).text;
            });
        },

        getCell: function (a, columnNumber) {
            if (a.id.match(/^jobNestedItems_/)) {
                var groupId = "category_" + a.id.replace(/^jobNestedItems_/, "");
                var categoryRow = document.getElementById(groupId);
                return categoryRow.cells[columnNumber]
            }

            return a.cells[columnNumber];
        },

        getIndexOfSortableTable: function () {
            return Array.from(document.querySelectorAll("TABLE.categorizedSortable")).indexOf(this.table);
        },

        getInnerText: function (el) {
            if (typeof el == "string") return el;
            if (typeof el == "undefined") {
                return el
            }
            if (el.innerText) return el.innerText; //Not needed but it is faster
            var str = "";

            var cs = el.childNodes;
            var l = cs.length;
            for (var i = 0; i < l; i++) {
                switch (cs[i].nodeType) {
                    case 1: //ELEMENT_NODE
                        str += this.getInnerText(cs[i]);
                        break;
                    case 3: //TEXT_NODE
                        str += cs[i].nodeValue;
                        break;
                }
            }
            return str;
        },

        // extract data for sorting from a cell
        extractData: function (x) {
            if (x == null) return '';
            var data = x.getAttribute("data");
            if (data != null)
                return data;
            return this.getInnerText(x);
        }
    };


    var arrowTable = {
        up: {
            id: "up",
            text: "&nbsp;&nbsp;&uarr;"
        },
        down: {
            id: "down",
            text: "&nbsp;&nbsp;&darr;"
        },
        none: {
            id: "none",
            text: "&nbsp;&nbsp;&nbsp;"
        },
        lnkRef: null
    };

    arrowTable.up.next = arrowTable.down;
    arrowTable.down.next = arrowTable.up;



    // available sort functions
    var sorter = {
        date: function (a, b) {
            function toDt(x) {
                // y2k notes: two digit years less than 50 are treated as 20XX, greater than 50 are treated as 19XX
                if (x.length == 10) {
                    return x.substr(6, 4) + x.substr(3, 2) + x.substr(0, 2);
                } else {
                    var yr = x.substr(6, 2);
                    if (parseInt(yr) < 50) {
                        yr = '20' + yr;
                    } else {
                        yr = '19' + yr;
                    }
                    return yr + x.substr(3, 2) + x.substr(0, 2);
                }
            }

            var dt1 = toDt(a);
            var dt2 = toDt(b);

            if (dt1 == dt2) return 0;
            if (dt1 < dt2) return -1;
            return 1;
        },

        currency: function (a, b) {
            a = a.replace(/[^0-9.]/g, '');
            b = b.replace(/[^0-9.]/g, '');
            return parseFloat(a) - parseFloat(b);
        },

        numeric: function (a, b) {
            a = parseFloat(a);
            if (isNaN(a)) a = 0;
            b = parseFloat(b);
            if (isNaN(b)) b = 0;
            return a - b;
        },

        caseInsensitive: function (a, b) {
            return sorter.fallback(a.toLowerCase(), b.toLowerCase());
        },

        fallback: function (a, b) {
            if (a == b) return 0;
            if (a < b) return -1;
            return 1;
        },

        /**
         * return the sorter to be used for the given value
         * @param {String} itm
         *      Text
         */
        determine: function (itm) {
            var sortfn = this.caseInsensitive;
            if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d\d\d$/)) sortfn = this.date;
            if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d$/)) sortfn = this.date;
            if (itm.match(/^[£$]/)) sortfn = this.currency;
            if (itm.match(/^-?[\d\.]+$/)) sortfn = this.numeric;
            return sortfn;
        },

        reverse: function (f) {
            return function (a, b) {
                return -f(a, b)
            };
        }
    };

    var storage;
    try {
        storage = YAHOO.util.StorageManager.get(
            YAHOO.util.StorageEngineHTML5.ENGINE_NAME,
            YAHOO.util.StorageManager.LOCATION_SESSION, {
                order: [
                    YAHOO.util.StorageEngineGears
                ]
            }
        );
    } catch (e) {
        // no storage available
        storage = {
            setItem: function () {},
            getItem: function () {
                return null;
            },
            hasKey: function () {
                return false;
            }
        };
    }

    return {
        CategorizedSortable: CategorizedSortable,
        sorter: sorter
    };
})();