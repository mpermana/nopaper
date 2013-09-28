(function (container) {
  'use strict';


  container.router = new (Backbone.Router.extend({
    routes : {
        'papertrader/:page' : function (page) {
            console.log(page);
        }
    }
  }))();

  container.papertrade = papertrade;

}(window.papertrade));


