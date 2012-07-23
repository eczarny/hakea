$(function() {
    function search() {
        $('#results').html('<br /><div class="progress progress-striped active" style="width: 35%; margin: 0px auto;"><div class="bar" style="width: 100%;"></div></div>');

        $.ajax({
            url: '/api/1.0/search',
            data: {
                'q': $('#q').val()
            },
            success: function(data) {
                var results = _.map(data, function(result) {
                    var html = '<h1>' + result.project_s + '</h1><i>' + result.ref_s + '</i>';

                    if (result.id.indexOf('commit') > -1) {
                        html = html + '<pre>' + result.commit_full_message_en + '</pre>' + '<pre class="prettyprint linenums pre-scrollable">' + _.escape(result.commit_diff_t) + '</pre>';
                    } else {
                        html = html + '<pre>' + result.file_path_s + '</pre>' + '<pre class="prettyprint linenums pre-scrollable">' + _.escape(result.file_content_t) + '</pre>';
                    }

                    return html;
                });

                $('#results').empty();

                _.each(results, function (result) {
                    $('#results').append(result);
                });

                prettyPrint();
            }
        });
    }

    $('#submit').bind('click', search);

    $('#q').bind('keypress', function(e) {
        var code = e.keyCode || e.which;

        if (code == 13) {
            search();
        }
    });

    $('#q').focus();
});
