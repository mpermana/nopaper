/*global np,dust,$,_,Backbone*/
/**
 * provide localized templating with dust
 * requires np.messages.js where the localized string is defined
 *
 * This module creates a baseContext where it loads everything from
 * english messages. A function to set a user locale creates a
 * localizedContext from the baseContext. Template render will be performed
 * on the localizedContext first then fall back to the baseContext.
 *
 * When doing rendering the Contex stack look like:
 *   contextBase = { MESSAGES.en-US, session, window }
 *     contextLocalized = { MESSAGES.ja-JP }
 *       data = { ... passed as function argument ... }
 */
np.template = (function () {
  'use strict';

  var
    contextBase, contextLocalized, cache = {};

  /** change templates to use the given locale */
  function changeLocale ( newLocale ) {
    var MESSAGES = np['MESSAGES-' + newLocale] || {};
    contextLocalized = contextBase.push( MESSAGES );
    cache = {};
  }

  /** render the given template_id and provide localized context */
  function render ( template_id, data ) {
    var result_string, json = data || {};

    if ( data && data instanceof Backbone.Model ) {
      json = data.toJSON();
    }

    dust.render( template_id, contextLocalized.push( json ),
      function ( err, out ) {
        result_string = out;
      } );
    return result_string;
  }

  function render$ ( template_id, data ) {
    var $result = $( '<div></div>' );
    $result.html( render( template_id, data ) );
    return $result;
  }

  /**
   * cache jquery object
   * if data is needed to render, then you dont want to cache,
   * use render/render$ instead of $render() */
  function $render ( template_id ) {
    var $result = cache[template_id];
    if ( !$result ) {
      $result = cache[template_id] = render$( template_id );
    }
    return $result;
  }

  function getMessage ( key ) {
    var result = contextLocalized.get( key.toUpperCase() );
    if ( result ) {
      return result;
    }

    throw "unlocalized key " + key.toUpperCase();
  }


  dust.onLoad = function ( name, callback ) {
    alert( 'template not found ' + name );
  };

  function dustCompile ( template, id ) {
    dust.loadSource( dust.compile( template, id ) );
  }

  function initModule() {
    _.each($('div.template div[id]'), function compile ( template_dom ) {
      // stupid dust shouldn't have used {> to start a partial
      // now we have to hack a {> which is escaped by html
      var html = $( template_dom ).html().replace( /\{&gt;/g, '{>' ),
        compiled = dust.compile( html, template_dom.id );
      console.log('loaded tmpl ' + template_dom.id);
      dust.loadSource( compiled );
    } );
    // Set up a base context with global helpers
    contextBase = dust.makeBase( _.extend( {
      session : np.session,
      window : window
    }, np.MESSAGES ) );
    contextLocalized = contextBase;

  }

  return {
    render       : render,
    render$      : render$,
    $render      : $render,
    changeLocale : changeLocale,
    getMessage   : getMessage,
    dustCompile  : dustCompile,
    initModule   : initModule
  };

}());

