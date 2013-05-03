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
    var
      $html = np.template.render$('debug-config')
      ;
    setContent( $html );
  }

  Router = Backbone.Router.extend( {
    routes               : {
      ''                                   : 'welcomeView', // matches http://example.com/#anything-here
      // user
      'user'                               : 'listUser',
      'user/:username'                     : 'viewUser',
      // unauthorized
      'unauthorized'                       : 'unauthorized',
      'config'                             : 'config_page'
    },
    welcomeView          : function () {
      var user_id = np.session.get('user_id');
      if (user_id) {
        setContent( np.views.welcomeView.$el );
	np.views.welcomeView.render();
      }
      else {
        this.navigate( 'unauthorized', true );
      }
    },
    unauthorized         : function () {
      if ( np.session.get('user_id') ) {
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
    $( document ).ajaxError( function ( e, xhr, options ) {
      try {
        var CONSTANTS = {
            HTTP_CLIENT_ERROR_BAD_REQUEST  : 400,
            HTTP_CLIENT_ERROR_UNAUTHORIZED : 401,
            HTTP_CLIENT_ERROR_NOT_FOUND    : 404
          }, $dialog_error = $( '<div class="np-x-error"></div>' ),
          parsedErrorMessage = '';
        if ( !xhr.status ) {
          // cross origin problem
          $( 'div.np-shell-content' ).html( xhr.statusText );
          $dialog_error.html( xhr.statusText );
          if ( xhr.statusText ===
            "Error: NETWORK_ERR: XMLHttpRequest Exception 101" ) {
            $dialog_error.append( 'You have cross origin problem, '
              + ' check http://developer.chrome.com/extensions/xhr.html<br>' );
          }
          $dialog_error.dialog( {
            width   : 600,
            height  : 400,
            modal   : true,
            buttons : {Ok : function () {
              $( this ).dialog( 'close' );
            }}} );
          return;
        }
        if ( xhr.status ===
          CONSTANTS.HTTP_CLIENT_ERROR_UNAUTHORIZED ) {
          // unauthorized
          window.location = '#unauthorized';
          return;
        }

        // do best effort attempt to parse error
        try {
          parsedErrorMessage =
            _.pluck( JSON.parse( xhr.responseText ).error_list,
              'error_msg' ).join();
        }
        catch ( parse_error ) {
          // This happens when the error is not from server.
          // For example when uploading > 20 MB file nginx returns the error
          // as html page in the xhr.responseText.
          parsedErrorMessage = xhr.responseText;
        }
        if ( xhr.status !== CONSTANTS.HTTP_CLIENT_ERROR_NOT_FOUND &&
          xhr.status >= CONSTANTS.HTTP_CLIENT_ERROR_BAD_REQUEST ) {
          np.model.application.error = {e : e, xhr : xhr, options : options};
          if ( xhr.responseText && !options.suppressErrorDialog ) {
            $dialog_error.html( parsedErrorMessage );
            $dialog_error.dialog( {
              width   : 600,
              modal   : true,
              buttons : {Ok : function () {
                $( this ).dialog( 'close' );
              }}} );
          }
        }
      }
      catch ( fatalError ) {
        np.model.application.log( fatalError.toString() );
        window.alert( xhr.responseText );
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

    np.session.on('change:user_id',function() {
      this.set({me:new np.model.me({_id:this.get('user_id')})});
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


