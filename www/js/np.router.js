/*global np,Backbone,_,$*/
np.router = (function () {
  'use strict';
  var jqueryMap = {}, router, Router;

  function setJqueryMap ( $content ) {
    jqueryMap.$content = $content;
  }

  function setContent ( content ) {
    jqueryMap.$content.empty().append( content );
  }

  function setContentWithAjaxLoader ( $content ) {
    // this loader added the sexy ajax loader
    setContent( $content );
    $content.html( np.template.$render( 'np-ajax-loader' ) );
  }

  function showLoadingLayer() {
    np.template.$render( 'np-ajax-loader' ).addClass('np-block-screen');
    jqueryMap.$content.append( np.template.$render( 'np-ajax-loader' ) );
  }

  function config_page() {
    var $html = np.template.render$('debug-config',np.session.toJSON());
    setContent($html);
  }

  function createRouterFunction(view) {
    return function () {
      setContent(view.$el);
      view.render();
    }
  }

  Router = Backbone.Router.extend( {
    routes               : {
      ''                                   : 'welcomeView', // matches http://example.com/#anything-here
      // user
      'pdf' : 'managePdf',
      'user/:username'                     : 'viewUser',
      // unauthorized
      'unauthorized'                       : 'unauthorized',
      'config'                             : 'config_page'
    },
    managePdf : function () {
    },
    welcomeView          : function () {
      var userId = np.session.get('userId');
      if (userId) {
        setContent( np.views.welcomeView.$el );
	np.views.welcomeView.render();
      }
      else {
        this.navigate( 'unauthorized', true );
      }
    },
    unauthorized         : function () {
      if ( np.session.get('userId') ) {
        this.navigate( '', true );
      }
      else {
        setContent( np.template.$render( 'welcomeUnauthenticated' ) );
      }
    },
    listUser             : function () {
      var subscriber_id = np.model.application.session.isAdministrator() ?
        null : np.model.application.session.get( 'active_subscriber_id' ),
        model = new np.data.user.List( { subscriber_id : subscriber_id } ),
        view = new np.view.user.ListView( { model : model} );
      setContent( view.el );
      model.fetch();
    },
    viewUser             : function ( username ) {
      var model = new np.data.user.Model( {username : username} ),
        view = new np.view.user.DetailView( {model : model} );
      setContent( view.el );
      model.fetch();
    },
    config_page          : config_page
  } );

  function setup_ajax_error () {
    $(document).ajaxError( function ( e, xhr, options ) {
      try {
        if (!options.suppressErrorDialog) {
            window.alert(xhr.responseText);
        }
      }
      catch (fatalError) {
        console.log(fatalError.toString());
      }
    } );
  }

  /** public */
  function navigate ( path, options ) {
    router.navigate( path, options );
  }

  function initModule ( $content ) {

    setJqueryMap( $content );

    setup_ajax_error();

    router = new Router();
    Backbone.history.start();

    np.session.on('change:userId',function() {
      this.set({me:new np.model.me({_id:this.get('userId')})});
      this.get('me').fetch().done(function() {
    	  router.navigate('',true);
      });
    });

    router.on( 'route', function ( page ) {
      //app.analytics.trackPageView( window.location.pathname + '#' + page );
    } );
  }

  return {
    initModule : initModule,
    navigate   : navigate
  };

}());
