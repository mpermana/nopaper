var np = (function () {
  'use strict';

  var
    fbClient;

  function showLogin(show) {
    if (show) {
      $('#fbLogin').show();
      $('#fbLogout').hide();
    } else {
      $('#fbLogin').hide();
      $('#fbLogout').show();
    }
  }

  function initModule() {
    np.template.initModule();
    np.views.initModule();
    np.router.initModule($('.content'));


    var headerView = new (Backbone.View.extend({
      el : $('.header'),
      events : {
        'click #fbLogin'                : 'fbLogin',
        'click #fbLogout'               : 'fbLogout'
      },
      fbLogin : function() {
        FB.login(np.fbClient.loginResponse, {scope:'email,user_likes,manage_pages,publish_stream'});
      },
      fbLogout : function() {
        FB.logout(function () {
          showLogin(true);
        });
      }
    }))();

  }

  fbClient = {
    loginResponse : function(response) {
      if (response.status === 'connected') {
          // connected
          // console.log(response.authResponse.accessToken);
        np.session.set({'user_id':response.authResponse.userID});
        showLogin(false);
      } else if (response.status === 'not_authorized') {
          // not_authorized
          showLogin(true);
      } else {
          // not_logged_in
          showLogin(true);
      }
    }
  };

  return {
    initModule : initModule,
    fbClient   : fbClient
  };
}());



$(function () {
  $('button').button();
  np.initModule();
});

window.fbAsyncInit = function () {
  FB.init({
      appId      : '361690837262157', // App ID from the App Dashboard
      channelUrl : '//taru.zeeses.com/map/channel.html', // Channel File for x-domain communication
      status     : true, // check the login status upon init?
      cookie     : true, // set sessions cookies to allow your server to access the session?
      xfbml      : true  // parse XFBML tags on this page?
  });
  // Additional initialization code such as adding Event Listeners goes here
  FB.getLoginStatus(np.fbClient.loginResponse);
}
