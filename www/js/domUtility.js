/**
 * Copyright © 2013, Michael Permana. All rights reserved.
 */
/*global $,Backbone,_*/
(function () {
  'use strict';

  /** enable enter on all input to call callback function */
  function enableEnter ( $el, callbackOnEnter ) {
    var
      $input = $el.find( 'input' );
    $input.off( 'keypress' );
    $input.keypress( function ( e ) {
      if ( e.which === 13 ) {
        callbackOnEnter();
      }
    } );
  }

  /**
   * Get the HTML controls (input,select,textarea) in a DOM element rooted at rootEl.
   * Create an empty javascript object { }
   * For each control, get the name and value of the control, and store it in the javascriptObject.
   * Returns the javascript object.
   * For example:
   *   <div class="loginForm">
   *     <input name="username" value="mpermana@paypal.com">
   *     <input name="password" value="11111111">
   *   </div>
   * $form = $('div.loginForm')
   * $.getHtmlInputValue($form) returns:
   * {
   *   username : 'mpermana',
   *   password : '11111111'
   * }
   * SAY BYE BYE TO $('input["username"]')
   */
  function getHtmlInputValue ( rootEl ) {

    var javascriptObject = {},
      is_optional;

    _.each( $( rootEl ).find( 'input,select,textarea' ),

      function ( control ) {

        var current = javascriptObject,
          path = control.name.split( '.' ),
          i = 0,
          key,
          $control, value;

        for ( i = 0; i < path.length - 1; i++ ) {
          key = path[i];
          if ( ! current[path[i]] ) {
            current[path[i]] = {};
          }
          current = current[path[i]];
        }

        if ( path[i] ) {
          if ( control.type === 'radio' && ! control.checked ) {
            return;
          }
          $control = $( control );
          if ( control.type === 'checkbox' ) {
            if ( ! control.checked ) {
              return;
            }
            value = control.value;
          }
          else {
            value = $.trim( $control.val() );
          }
          if ( ! _.isEmpty( value ) ||
            ! $control.hasClass( 'sla-util_b-optional' ) ) {
            if ( 'integer' === $control.attr( 'data-type' ) ) {
              value = parseInt( value, 10 );
            }
            if ( 'array' === $control.attr( 'data-type' ) ) {
              if ( undefined === current[path[i]] ) {
                current[path[i]] = [ ];
              }
              if ( ! _.isEmpty( value ) || _.isBoolean( value ) ) {
                current[path[i]].push( value );
              }
            }
            else {
              if ( undefined === current[path[i]] ) {
                current[path[i]] = value;
              }
              else {
                // muliple value with same name, turn it into array
                if ( ! _.isArray( current[path[i]] ) ) {
                  current[path[i]] = [current[path[i]]];
                }
                // now you have an array you can push into it
                current[path[i]].push( value );
              }
            }
          }
        }
      } );

    return javascriptObject;
  }

  /**
   * Set html control's input value (see getHtmlInputValue)
   */
  function setHtmlInputValue ( rootEl, javascriptObject ) {
    _.each( $( rootEl ).find( 'input,select' ), function ( control ) {
      var current = javascriptObject,
        path = control.name.split( '.' ),
        i = 0,
        key,
        $control;
      for ( i = 0; i < path.length - 1; i++ ) {
        key = path[i];
        if ( ! current[path[i]] ) {
          current[path[i]] = {};
        }
        current = current[path[i]];
      }
      if ( path[i] && current[path[i]] ) {
        $control = $( control );
        if ( control.type === 'checkbox' ) {
          if ( _.isArray( current[path[i]] ) ) {
            control.checked = _.contains( current[path[i]], control.value );
          }
          else {
            control.checked = control.value === current[path[i]];
          }
        }
        else if ( control.type === 'radio' ) {
          $control.attr( 'checked', $control.val() === current[path[i]] );
        }
        else {
          $control.val( current[path[i]] );
        }
      }
    } );
  }

  function setVisible( $control, show ) {
    if ( show ) {
      $control.show();
    }
    else {
      $control.hide();
    }
  }

  window.domUtility = {
    enableEnter            : enableEnter,
    getHtmlInputValue      : getHtmlInputValue,
    setHtmlInputValue      : setHtmlInputValue,
    setVisible             : setVisible
  };

}());

function echo() {
  _.each(arguments,function (a) {
    console.log(a);
  });
}
