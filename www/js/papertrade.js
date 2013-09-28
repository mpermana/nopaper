(function (container) {
  'use strict';

  var
    application = {},
    fbClient
    ;

   container.application = application;


  function init() {
    Backbone.history.start();
  }

}(window));

$(function () {
  window.application.init();
});

