define([
    //Common libraries
    "common",
    "bonzo",
    "domReady",
    //Modules
    "modules/trailblocktoggle",
    "modules/trailblock-show-more",
    "modules/footballfixtures",
    "modules/cricket"
], function (
    common,
    bonzo,
    domReady,
    TrailblockToggle,
    TrailblockShowMore,
    FootballFixtures,
    Cricket
) {

    var modules = {

        showCricket: function(){
            common.mediator.on('page:front:ready', function(config, context) {
                Cricket.cricketTrail(config, context);
            });
        },
            
        showTrailblockToggles: function () {
            var tt = new TrailblockToggle();
            common.mediator.on('page:front:ready', function(config, context) {
                tt.go(config, context);
            });
        },

        showTrailblockShowMore: function () {
            var trailblockShowMore = new TrailblockShowMore();
            common.mediator.on('page:front:ready', function(config, context) {
                trailblockShowMore.init(context);
            });
        },

        promoteMostPopular: function () {
            common.mediator.on('page:front:ready', function(config, context) {
                if (context.querySelector('.front-container--new') && window.location.pathname === '/') {
                    bonzo(context.querySelector('.js-popular'))
                        .appendTo(bonzo.create('<section class="front-section">'))
                        .insertAfter(context.querySelector('section.front-section'));
                }
            });
        },

        showFootballFixtures: function(path) {
            common.mediator.on('page:front:ready', function(config, context) {
                if(config.page.edition === "UK") {

                    var opts,
                        table;

                    switch(window.location.pathname) {
                        case "/" :
                            opts = {
                                prependTo: context.querySelector('.zone-sport ul > li'),
                                competitions: ['500', '510', '100'],
                                contextual: false,
                                expandable: true,
                                numVisible: 3
                            };
                            break;
                        case "/sport" :
                            opts = {
                                prependTo: context.querySelector('.trailblock ul > li'),
                                contextual: false,
                                expandable: true,
                                numVisible: 5
                            };
                            break;
                    }

                    if(opts && !bonzo(opts.prependTo).hasClass('footballfixtures-loaded')) {
                        bonzo(opts.prependTo).addClass('footballfixtures-loaded');
                        table = new FootballFixtures(opts).init();
                    }
                }
            });
        }
        
    };

    var ready = function (config, context) {
        if (!this.initialised) {
            this.initialised = true;
            modules.promoteMostPopular();
            modules.showTrailblockToggles();
            modules.showTrailblockShowMore();
            modules.showFootballFixtures();
            modules.showCricket();
        }
        common.mediator.emit("page:front:ready", config, context);
    };

    return {
        init: ready
    };

});
