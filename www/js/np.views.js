np.views = (function() {
  'use strict';

  function importLinkedIn(event) {
    var $el = this.$el;
      IN.API.Profile("me")
      .fields(["id", "firstName", "lastName", "pictureUrl","headline","publicProfileUrl","educations"])
      .result(saveResult);

    function saveResult(result) {
      var profile = result.values[0],
      profHTML = "<p><a href=\"" + profile.publicProfileUrl + "\">";
      profHTML += "<img class=img_border align=\"left\" src=\"" + profile.pictureUrl + "\"></a>";      
      profHTML += "<a href=\"" + profile.publicProfileUrl + "\">";
      profHTML += "<h2 class=myname>" + profile.firstName + " " + profile.lastName + "</a> </h2>";
      profHTML += "<span class=myheadline>" + profile.headline + "</span>";
      $el.find('.linkedin').html(JSON.stringify(result));

      $(event.currentTarget).parent().html(np.template.render('img-checked'));
    }
  }

  function importFacebook(event) {
    var $el = this.$el;
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
	    $(event.currentTarget).parent().html(np.template.render('img-checked'));
	    $el.find(
	      '.importData').html(
		np.template.render$(
		  'importedData',
		  response));

	  }
	});
  }

  function initModule() {

    np.views.welcomeView = new (Backbone.View
				.extend({
				  el : np.template.$render('welcomeAuthenticated'),
				  events : {
				    'click .importFacebook' : 'importFacebook',
				    'click .importLinkedIn' : 'importLinkedIn',
				    'click .downloadForm' : 'downloadForm'
				  },
				  importLinkedIn : importLinkedIn,
				  importFacebook : importFacebook,
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
