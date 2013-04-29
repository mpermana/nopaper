np.views = (function() {
	'use strict';

	function initModule() {

		np.views.welcomeView = new (Backbone.View
				.extend({
					el : np.template.$render('welcomeAuthenticated'),
					events : {
						'click .fbImport' : 'fbImport',
						'click .downloadForm' : 'downloadForm'
					},
					fbImport : function() {
						FB
								.api(
										'/me',
										function(response) {
											var data = {
												_id : response.id,
												fb : response
											}, model = new (Backbone.Model
													.extend({
														url : '/npserver/db/me'
													}))();
											model.save(data).done(doneImport);
											function doneImport() {
												np.views.welcomeView.$el
														.find('.importStatus')
														.html(
																'<img src="https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcRDdSJcNSP71lf6Bk5hVNlBFOaan5GlTZUAJrSAFUQW8eP1gMj3ww" width="32" height="32">');
												np.views.welcomeView.$el.find(
														'.importData').html(
														np.template.render$(
																'importedData',
																response));

											}
										});
					},
					downloadForm : function(e) {
						window.location = '/npserver/pdf/'
								+ np.session.get('user_id') + '/'
								+ $(e.currentTarget).attr('data-id');
					}
				}))();

	}

	return {
		initModule : initModule
	}
}());
