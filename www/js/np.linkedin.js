(function () {

  function onLinkedInLoad() {
    np.session.set({in_id:IN.User.getMemberId()});
  }

  np.linkedin = {
    onLinkedInLoad : onLinkedInLoad
  };

}());