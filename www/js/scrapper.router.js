/*global scrapper,Backbone,_,$*/
if (!window.scrapper) window.scrapper = {};
scrapper.router = (function () {
  'use strict';
  var jqueryMap = {}, router = null;

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

  /** public */
  function initModule ( $content ) {
    setJqueryMap( $content );
    router = new (Backbone.Router.extend({
	    routes : {
	        'scrapper' : function() {
	        	var view = scrapper.views.getView('UserListView');
	        	setContent(view.$el);
	        	view.render();
	        }
	    }
    }))();
  }

  function navigate ( path, options ) {
    router.navigate( path, options );
  }

  return {
    initModule : initModule,
    navigate   : navigate
  };

}());


