/*global scrapper,Backbone,_,$*/
if (!window.scrapper)
	window.scrapper = {};
scrapper.views = (function() {
	'use strict';

	var viewsCache = {}, views = {};

	var User = Backbone.Model
			.extend({
				urlRoot : '/npserver/db/scrap',
				idAttribute : '_id',
				validate : function(attrs) {
					var properties = [ 'userId', 'firstName', 'lastName',
							'email' ], error = '';
					_.each(properties, function(property) {
						if (!attrs[property]) {
							error += 'Missing ' + property + ',';
						}
					});
					return error;
				},
				initialize : function() {
					console.log('user initialize');
					this.on('invalid', function(a1, a2, a3) {
						alert(a2);
					});
				}
			});

	function setEditorDisplay($container, display) {
		$container.find('.editor').css('display', display ? '' : 'none');
		$container.find('.readOnly').css('display', !display ? '' : 'none');
	}

	function addRow($table, data, show) {
		var row, $nodes;
		row = $table.dataTable().fnAddData(data)[0];
		$nodes = $($table.fnGetNodes(row));
		setEditorDisplay($nodes, show);
	}

	function addUser() {
		var newUser = {
			userId : 'new',
			firstName : 'new',
			lastName : 'new',
			email : 'email'
		};
		addRow(this.$table, newUser, true);
	}

	function scrapUser() {

	}

	function deleteRow(e) {
		var $table = this.$table, position, user;
		position = $table.fnGetPosition(e.currentTarget.parentNode);
		user = new User({
			_id : $(e.currentTarget).attr('data-id')
		});
		user.destroy().done(function() {
			$table.fnDeleteRow(position[0]);
		});
	}

	function cancel(e) {
		var position = this.$table.fnGetPosition(e.currentTarget.parentNode), id = $(
				e.currentTarget).attr('data-id');
		this.$table.fnDeleteRow(position[0]);
		console.log(id);

	}

	function edit(e) {
		var $tr;
		$tr = $($(e.currentTarget).parents('tr')[0]);
		setEditorDisplay($tr, true);
	}

	function saveRow(e) {
		var position, $tr, user_data, user, $table;

		position = this.$table.fnGetPosition(e.currentTarget.parentNode);
		$tr = $(e.currentTarget).parents('tr')[0];
		user_data = window.domUtility.getHtmlInputValue($tr);
		user = new User(this.$table.fnGetData()[position[0]]);
		$table = this.$table;

		user.save(user_data, {
			success : function() {
				$table.fnDeleteRow(position[0]);
				addRow($table, user.toJSON());
			},
			error : function() {
				console.log('error');
			}
		});
	}

	views.UserListView = {
		initialize : function initialize() {
			this.$table = this.$el.find('table');
			function createRenderer(propertyName) {
				return function(data, type, full) {
					var readonly, editor;

					readonly = '<div class="readOnly">' + data + '</div>';
					editor = '<div class="editor" style="display:none"><input name="'
							+ propertyName + '" value="' + data + '"></div>';
					return readonly + editor;
				};

			}
			this.$table
					.dataTable({
						'bProcessing' : true,
						'sAjaxSource' : '/npserver/db/scrap',
						'sAjaxDataProp' : '',
						"aoColumns" : [
								{
									"mData" : "userId",
									"mRender" : function(data, type, full) {
										if (full._id) {
											return '<a class="delete" data-id="'
													+ full._id
													+ '">x</a> <a class="edit readOnly">Edit</a> <a class="save editor" style="display:none">Save</a>';
										} else {
											return '<a class="delete" data-id="'
													+ full._id
													+ '">Cancel</a><a class="save">Save</a>';
										}
									}
								}, {
									"mData" : "userId",
									mRender : createRenderer('userId')
								}, {
									"mData" : "firstName",
									mRender : createRenderer('firstName')
								}, {
									"mData" : "lastName",
									mRender : createRenderer('lastName')
								}, {
									"mData" : "email",
									mRender : createRenderer('email')
								} ]
					});
		},
		events : {
			'click .addUser' : addUser,
			'click .scrapUser' : scrapUser,
			'click .delete' : deleteRow,
			'click .cancel' : cancel,
			'click .save' : saveRow,
			'click .edit' : edit
		}
	};

	function getView(viewName) {
		var view = viewsCache[viewName];
		if (!view) {
			view = viewsCache[viewName] = new (Backbone.View.extend(_.extend({
				el : np.template.$render(viewName)
			}, views[viewName])))();
		}
		return view;
	}

	return {
		getView : getView
	};
}());

function getTable() {
	return scrapper.views.getView('UserListView').$table;
}
