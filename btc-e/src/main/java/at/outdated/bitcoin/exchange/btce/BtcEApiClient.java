package at.outdated.bitcoin.exchange.btce;

import at.outdated.bitcoin.exchange.api.OrderId;
import at.outdated.bitcoin.exchange.api.client.ExchangeApiClient;
import at.outdated.bitcoin.exchange.api.market.Market;
import at.outdated.bitcoin.exchange.api.account.AccountInfo;
import at.outdated.bitcoin.exchange.api.currency.Currency;
import at.outdated.bitcoin.exchange.api.currency.CurrencyValue;
import at.outdated.bitcoin.exchange.api.market.*;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


// TODO: use fee api: https://hdbtce.kayako.com/Knowledgebase/Article/View/27/4/api-fee
//

public class BtcEApiClient extends ExchangeApiClient {

    public BtcEApiClient(Market market) {
        super(market);
    }

    @Override
    public AccountInfo getAccountInfo() {



        //https://btc-e.com/tapi/
        WebTarget tgt = client.target("https://btc-e.com/tapi");

        MultivaluedMap<String,String> data = null;

        data = new MultivaluedHashMap<>();
        data.add("method", "getInfo");
        String raw = protectedPostRequest(tgt, String.class, Entity.form(data));
        //log.debug("raw info: {}", raw);
        InfoResponse infoRes = BtcEJsonResolver.convertFromJson(raw, InfoResponse.class);

        AccountInfo info = infoRes.result;

        data = new MultivaluedHashMap<>();
        data.add("method", "TransHistory");
        raw = protectedPostRequest(tgt, String.class, Entity.form(data));

        //log.debug("raw transactions: {}", raw);
        JsonObject transResponse = jsonFromString(raw);
        if(transResponse.getInt("success") == 1) {
            /*
            {
                "success":1,
                "return":{
                    "1081672":{
                        "type":1,
                        "amount":1.00000000,
                        "currency":"BTC",
                        "desc":"BTC Payment",
                        "status":2,
                        "timestamp":1342448420
                    }
                }
            }
            */

            //FIXME: this doesn't do anyting: account data parsing
            JsonObject transResult = transResponse.getJsonObject("result");
            for(String key : transResult.keySet()) {
                JsonObject jt = transResult.getJsonObject(key);
                Currency curr = Currency.valueOf(jt.getString("currency"));

                double volume = jt.getJsonNumber("amount").doubleValue();
                Date timestamp = new Date(jt.getJsonNumber("timestamp").longValue() * 1000L);
                String desc = jt.getString("desc");

            }
        }



        data = new MultivaluedHashMap<>();
        data.add("method", "TradeHistory");
        raw = protectedPostRequest(tgt, String.class, Entity.form(data));
        //log.debug("raw trades: {}", raw);
        JsonObject tradeResponse = jsonFromString(raw);
        if(tradeResponse.getInt("success") == 1) {
        /*
                {
            "success":1,
            "return":{
                "166830":{
                    "pair":"btc_usd",
                    "type":"sell",
                    "amount":1,
                    "rate":1,
                    "order_id":343148,
                    "is_your_order":1,
                    "timestamp":1342445793
                }
            }
        }  */

            JsonObject jsonOrders = tradeResponse.getJsonObject("return");
            for(String key : jsonOrders.keySet()) {

                JsonObject jsonOrder = jsonOrders.getJsonObject(key);
                MarketOrder order = parseOrder(jsonOrder);

                order.setId(new OrderId(market, jsonOrder.getString("order_id")));
            }

        }


        return info;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MarketDepth getMarketDepth(AssetPair asset) {
        Currency base = asset.getBase();
        Currency quote = asset.getQuote();

        // (price, volume)

        WebTarget resource = client.target("https://btc-e.com/api/2/{base}_{quote}/depth")
            .resolveTemplate("base", base.name().toLowerCase())
            .resolveTemplate("quote", quote.name().toLowerCase());

        String response = super.simpleGetRequest(resource, String.class);

        JsonReader reader = Json.createReader(new StringReader(response));

        JsonObject root = reader.readObject();
        JsonArray asksArr = root.getJsonArray("asks");
        JsonArray bidsArr = root.getJsonArray("bids");

        MarketDepth depth = new MarketDepth(asset);

        for(int i=0; i<asksArr.size(); i++ ) {
            double price = asksArr.getJsonArray(i).getJsonNumber(0).doubleValue();
            double volume = asksArr.getJsonArray(i).getJsonNumber(1).doubleValue();

            depth.addAsk(volume, price);
        }
        for(int i=0; i<bidsArr.size(); i++ ) {
            double price = bidsArr.getJsonArray(i).getJsonNumber(0).doubleValue();
            double volume = bidsArr.getJsonArray(i).getJsonNumber(1).doubleValue();

            depth.addBid(volume, price);
        }

        return depth;
    }


    @Override
    protected <R> R simpleGetRequest(WebTarget target, Class<R> resultClass) {

        R result = null;

        String resultStr = super.simpleGetRequest(target, String.class);

        //log.debug("BTC-E raw: " + resultStr);
        result = BtcEJsonResolver.convertFromJson(resultStr, resultClass);

        return result;
    }

    @Override
    public TickerValue getTicker(AssetPair asset) {

        // https://btc-e.com/api/2/btc_usd/ticker

        WebTarget tickerResource = client.target("https://btc-e.com/api/2/" + asset.getBase().name().toLowerCase() + "_" + asset.getQuote().name().toLowerCase() + "/ticker");

        TickerResponse response = simpleGetRequest(tickerResource, TickerResponse.class);

        BtcETickerValue btcETickerValue = response.getTicker();

        TickerValue value = btcETickerValue.getTickerValue();
        value.setAsset(asset);

        return value;
    }



    @Override
    public Number getLag() {
        return 0.12345678910;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected <T> Invocation.Builder setupProtectedResource(WebTarget tgt, Entity<T> entity) {

        String apiKey = getUserId();
        String apiSecret = getSecret();

        Invocation.Builder builder = null;

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_spec = new SecretKeySpec(apiSecret.getBytes("UTF-8"), "HmacSHA512");
            mac.init(secret_spec);
            //    1.381.050.637
            long nonce = (new Date()).getTime()/1000L; //max:2.147.483.647

            Entity<Form> e = (Entity<Form>) entity;
            e.getEntity().param("nonce", Long.toString(nonce));

            String payload = formData2String(e.getEntity());
            log.debug("encoded payload: {}", payload);

            byte[] rawSignature = mac.doFinal(payload.getBytes("UTF-8"));
            String signature = new String(Hex.encodeHex(rawSignature));

            builder = tgt.request();

            builder.header("Key", apiKey);
            log.debug("Key: {}", apiKey);

            builder.header("Sign", signature);
            log.debug("Sign: {}", signature);
        }
        catch (Exception e) {
            log.error("error: {}", e);
        }

        return builder;
    }


    @Override
    public boolean cancelOrder(OrderId order) {
        // CancelOrder

        WebTarget tgt = client.target("https://btc-e.com/tapi");

        Form data = new Form();

        data.param("method", "CancelOrder");
        data.param("order_id", order.getIdentifier());

        String raw = protectedPostRequest(tgt, String.class, Entity.form(data));

        JsonObject jsonResponse = jsonFromString(raw);

        if(jsonResponse.getInt("success") == 0) {
            log.error("failed to cancel order: {}", order.getIdentifier());
            return false;
        }


        int cancelledId = jsonResponse.getJsonObject("return").getInt("order_id");
        if(cancelledId == Integer.parseInt(order.getIdentifier())) {
            return true;
        }

        log.error("failed to cancel order: {} (unwknown)", order.getIdentifier());

        return false;
    }

    @Override
    public OrderId placeOrder(AssetPair asset, TradeDecision decision, CurrencyValue volume, CurrencyValue price) {


        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(15);
        nf.setMaximumFractionDigits(8);
        nf.setMaximumIntegerDigits(15);


        // method: Trade

        WebTarget tgt = client.target("https://btc-e.com/tapi");

        Form data = new Form();
        data.param("method", "Trade");


        String pairStr = asset.getBase().name().toLowerCase() + "_" + asset.getQuote().name().toLowerCase();

        data.param("pair", pairStr); // btc_usd
        data.param("type", decision.name().toLowerCase());
        data.param("rate", nf.format(price.getValue()));
        data.param("amount", nf.format(volume.getValue()));

        String raw = protectedPostRequest(tgt, String.class, Entity.form(data));

        /*
            {
        "success":1,
            "return":{
                "received":0.1,
                "remains":0,
                "order_id":0,
                "funds":{
                    "usd":325,
                    "btc":2.498,
                    "sc":121.998,
                    "ltc":0,
                    "ruc":0,
                    "nmc":0
                }
            }
        }
     */
        JsonObject jsonOrderResult = jsonFromString(raw);

        if(jsonOrderResult.getInt("success") == 0) {
            log.error("failed to place order");
            return null;
        }

        return new OrderId(market, jsonOrderResult.getJsonObject("return").getInt("order_id") + "");
    }

    @Override
    public List<MarketOrder> getOpenOrders() {

        // ActiveOrders

        /*
        {
	"success":1,
	"return":{
		"343152":{
			"pair":"btc_usd",
			"type":"sell",
			"amount":1.00000000,
			"rate":3.00000000,
			"timestamp_created":1342448420,
			"status":0
		}
	}
}
 */

        WebTarget tgt = client.target("https://btc-e.com/tapi");

        Form data = new Form();
        data.param("method", "ActiveOrders");
        //data.param("pair", "CancelOrder"); // btc_usd

        String raw = protectedPostRequest(tgt, String.class, Entity.form(data));


        JsonObject jsonResult = jsonFromString(raw);
        if(jsonResult.getInt("success") == 0) {
            log.error("failed to get active orders");
            return null;
        }

        List<MarketOrder> orders = new ArrayList<>();

        JsonObject jsonOrders = jsonResult.getJsonObject("return");
        for(String key : jsonOrders.keySet()) {



            MarketOrder order = parseOrder(jsonOrders.getJsonObject(key));
            order.setId(new OrderId(market, key));

            orders.add(order);
        }

        return orders;
    }


    private MarketOrder parseOrder(JsonObject jsonOrder) {
        /*
        		"343152":{
			"pair":"btc_usd",
			"type":"sell",
			"amount":1.00000000,
			"rate":3.00000000,
			"timestamp_created":1342448420,
			"status":0
		}
         */
        MarketOrder order = new MarketOrder();

        String[] parts = jsonOrder.getString("pair").split("_");

        Currency left = Currency.valueOf(parts[0].toUpperCase());
        Currency right = Currency.valueOf(parts[1].toUpperCase());

        AssetPair asset = market.getAsset(left, right);
        order.setAsset(asset);

        String typeStr = jsonOrder.getString("type");
        order.setDecision(TradeDecision.valueOf(typeStr.toUpperCase()));


        double price = jsonOrder.getJsonNumber("rate").doubleValue();
        order.setPrice(new CurrencyValue(price, right));


        double volume = jsonOrder.getJsonNumber("amount").doubleValue();
        order.setVolume(new CurrencyValue(volume, left));

        return order;
    }

}
