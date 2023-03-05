package model.database;

import model.game.ChestRequest;

import static com.shurablack.sql.FluentSqlBuilder.*;

public class Statement {

    public static String INSERT_SONG(String userid, String playlist, String song) {
        return create().INSERT("playlist", VALUE(STR(userid), STR(playlist), STR(song))).toString();
    }

    public static String SELECT_COUNT_PLAYLIST(String userid) {
        return create().SELECT(COUNT("songlist",DISTINCT)).FROM("playlist")
                .WHERE(CONDITION("userid",EQUAL,STR(userid))).toString();
    }

    public static String SELECT_ALL_SONGS(String userid) {
        return create().SELECT("*").FROM(AS("playlist","p"))
                .WHERE(CONDITION("p.userid",EQUAL,STR(userid))).toString();
    }

    public static String SELECT_SONGS_OF_PLAYLIST(String userid, String playlist) {
        return create().SELECT("song").FROM("playlist").WHERE(
                CONDITION("userid",EQUAL,STR(userid)), AND("songlist",EQUAL,STR(playlist))).toString();
    }

    public static String SELECT_PLAYLISTS(String userid) {
        return create().SELECT_DISTINCT("songlist").FROM("playlist")
                .WHERE(CONDITION("userid",EQUAL,STR(userid))).toString();
    }

    public static String SELECT_SONG_PLAYLIST(String userid, String playlist) {
        return create().SELECT("songlist","song","id").FROM("playlist").WHERE(
                CONDITION("userid",EQUAL,STR(userid)), AND("songlist",EQUAL,STR(playlist))).toString();
    }

    public static String DELETE_SONG(String userid, String id) {
        return create().DELETE("playlist").WHERE(
                CONDITION("userid",EQUAL,STR(userid)),AND("id",EQUAL,STR(id))).toString();
    }

    public static String DELETE_ALL_SONGS(String userid) {
        return create().DELETE("playlist").WHERE(CONDITION("userid",EQUAL,STR(userid))).toString();
    }

    /*----------------------------------------------------------------------------------------------------------------*/

    /*-- Prepared Statments Ruler ------------------------------------------------------------------------------------*/

    public static String INSERT_RULER(String userid) {
        return create().INSERT("roles", VALUE(STR(userid), "false", NULL)).toString();
    }

    public static String SELECT_RULER(String userid) {
        return create().SELECT("*").FROM("roles").WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_RULER(String userid, boolean request, String role) {
        return create().UPDATE("roles").SET("request="+request)
                .WHERE("userid",EQUAL,STR(userid)).APPEND(","+(role == null ? "null" : "'" + role + "'")).toString();
    }

    /*----------------------------------------------------------------------------------------------------------------*/

    /*-- Prepared Statments Game -------------------------------------------------------------------------------------*/

    public static String INSERT_GAME(String userid) {
        return create().CALL(FUNCTION("INSERT_GAME",STR(userid))).toString();
    }

    public static String SELECT_GAME(String userid) {
        return create().SELECT("*").FROM("game").WHERE(CONDITION("userid",EQUAL,STR(userid))).toString();
    }

    public static String SELECT_GAME_FULL(String userid) {
        return create().SELECT("*").APPEND("FROM (SELECT game.userid, game.daily, game.booster, game.daily_bonus, " +
                "game.money, game.select_deck, game.deck, rating.blackjack, rating.chest FROM game " +
                "LEFT JOIN rating ON game.userid = rating.userid) AS sub").WHERE(
                        CONDITION("sub.userid",EQUAL,STR(userid))).toString();
    }

    public static String UPDATE_GAME_DAILY(String userid, long value) {
        return create().UPDATE("game").SET("daily=FALSE", "money=money+"+value).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_GAME_MONEY(String userid, long value) {
        return create().UPDATE("game").SET("money=money+"+value).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String GAME_DAILY() {
        return create().CALL(FUNCTION("GAME_DAILY","")).toString();
    }

    public static String UPDATE_GAME_DAILY_RESET() {
        return create().UPDATE("game").SET("daily=TRUE").WHERE(CONDITION("daily",EQUAL,"FALSE")).toString();
    }

    public static String UPDATE_GAME_BOOST_RESET() {
        return create().UPDATE("game").SET("booster=''").WHERE("booster"," NOT LIKE ", "''").toString();
    }

    public static String UPDATE_GAME_CHEST_RATING() {
        return create().UPDATE("rating").SET("chest=chest-2.0").WHERE("chest",GREATER,"500.0").toString();
    }

    public static String UPDATE_GAME_BLACKJACK(String userid, String rating, long value) {
        return create().UPDATE("game","raiting").SET("rating.blackjack="+FUNCTION("CONSTRAIN_RATING",rating)
                ,"game.money=game.money+"+value).WHERE("game.userid",EQUAL,STR(userid),
                AND("rating.userid",EQUAL,STR(userid))).toString();
    }

    public static String UPDATE_GAME_CHEST(String userid, String increase) {
        return create().UPDATE("rating").SET("chest=chest"+increase).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_GAME_BOOST(String userid, String callerid, String booster) {
        return create().UPDATE("game").SET("money=money+500","booster="+booster+callerid+",")
                .WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String SELECT_ORDER_GAME_MONEY() {
        return create().SELECT("*").FROM("game").ORDER("money",DESC).LIMIT(3).toString();
    }

    public static String SELECT_RATING() {
        return create().SELECT("*").FROM("rating").toString();
    }

    public static String UPDATE_GAME_DAILY_BONUS(String userid, int value) {
        return create().UPDATE("game").SET("daily_bonus=daily_bonus+"+value)
                .WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_GAME_DECKS(String userid, String decks) {
        return create().UPDATE("game").SET("deck="+STR(decks)).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_GAME_SELECT_DECK(String userid, String deck) {
        return create().UPDATE("game").SET("select_deck="+STR(deck)).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String UPDATE_GAME_CHEST(String userid, long money, String chestRating) {
        return create().UPDATE("game","rating").SET("game.money=game.money+"+money,
                "rating.chest="+FUNCTION("CONSTRAIN_RATING","rating.chest" + chestRating))
                .WHERE("game.userid",EQUAL,STR(userid), AND("rating.userid",EQUAL,STR(userid))).toString();
    }

    public static String UPDATE_GAME_CHEST(String userid, long money, String decks, String chestRating) {
        return create().UPDATE("game","rating").SET("game.deck="+STR(decks),"game.money=game.money+"+money,
                "rating.chest="+FUNCTION("CONSTRAIN_RATING","rating.chest" + chestRating))
                .WHERE("game.userid",EQUAL,STR(userid), AND("rating.userid",EQUAL,STR(userid))).toString();
    }

    public static String UPDATE_GAME_CHEST(String userid, long money, long daily_bonus, String chestRating) {
        return create().UPDATE("game","rating").SET("game.money=game.money+"+money,"game.daily_bonus=game.daily_bonus+"
                +daily_bonus,"rating.chest="+FUNCTION("CONSTRAIN_RATING","rating.chest"+chestRating))
                .WHERE("game.userid",EQUAL,STR(userid), AND("rating.userid",EQUAL,STR(userid))).toString();
    }

    public static String SELECT_GAME_COUNT() {
        return create().SELECT(COUNT("*","")).FROM("game").toString();
    }

    public static String UPDATE_GAME_BUNDLE(ChestRequest request) {
        final double chestAmount = ChestRequest.getRating(request);
        return create().UPDATE("game, rating").SET("game.deck="+STR((request.getCreator().getDeck()
                +request.getDecks())),"game.money=game.money+"+request.getMoney(),"game.daily_bonus=game.daily_bonus+"
                +request.getDaily(),"rating.chest="+FUNCTION("CONSTRAIN_RATING","rating.chest+"
                +String.valueOf(chestAmount).replace(",","."))).WHERE("game.userid",EQUAL,
                STR(request.getCreator().getUserid()), AND("rating.userid",EQUAL,STR(request.getCreator().getUserid()))).toString();
    }

    /*----------------------------------------------------------------------------------------------------------------*/

    /*-- Prepared Statments Time -------------------------------------------------------------------------------------*/

    public static String INSERT_USER_TIME(String userid, long time_sec) {
        return create().INSERT("usertime", VALUE(STR(userid), "" + time_sec)).toString();
    }

    public static String SELECT_USER_TIME(String userid) {
        return create().SELECT("*").FROM("usertime").WHERE(CONDITION("userid",EQUAL,STR(userid))).toString();
    }

    public static String UPDATE_USER_TIME(String userid, long time_sec) {
        return create().UPDATE("usertime").SET("time_sec+="+time_sec).WHERE("userid",EQUAL,STR(userid)).toString();
    }

    public static String SELECT_USER_TIME_TOP() {
        return create().SELECT("*").FROM("usertime").ORDER("time_sec",DESC).toString();
    }

}
