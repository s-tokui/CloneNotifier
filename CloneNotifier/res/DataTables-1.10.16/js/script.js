$.fn.dataTable.ext.search.push(
    function( settings, data, dataIndex ) {
		var risk = parseFloat( data[2] ) || 0; // use data for the age column
 
        if ( risk == 0)
        {
            return !$('#isDisplayRisk').prop('checked');
        }
        return true;
    }
);

$(document).ready(function() {
    var table = $('#myTable').DataTable({
		displayLength: 50 
    });

	$('#isDisplayRisk').click(function() {
		table.draw();
	});
} );


