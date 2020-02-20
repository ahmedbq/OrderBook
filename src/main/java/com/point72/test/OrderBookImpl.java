package com.point72.test;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OrderBookImpl implements OrderBook {
	
	Map<Double, Integer> buyingPriceToQuantity = new HashMap<>();
	Map<Double, Integer> sellingPriceToQuantity = new HashMap<>();

	@Override
	public void buy(double limitPrice, int quantity) {
		// Handling edge cases
		if (limitPrice <= 0 || quantity <= 0) {
			return;
		}
		
		// Sorting ascending since you would want to rid of the lowest offers first
		Set<Double> relevantOfferPrices = sellingPriceToQuantity.keySet().stream().sorted().filter(f -> f <= limitPrice).collect(Collectors.toSet());
		// Execute the trading and return the trading count
		int tradingCount = trade(sellingPriceToQuantity, relevantOfferPrices, quantity);
		
		int quantityLeft = quantity - tradingCount;

		// Only update the buying price list if there's any quantity left to add to the buying price map
		updatePriceQuantity(buyingPriceToQuantity, quantityLeft, limitPrice);
		
	}

	@Override
	public void sell(double limitPrice, int quantity) {
		// Handling edge cases
		if (limitPrice <= 0 || quantity <= 0) {
			return;
		}
		
		// Sorting in reverse order since you would want to get rid of the highest bidders first
		Set<Double> relativeBidPrices = buyingPriceToQuantity.keySet().stream().sorted(Comparator.reverseOrder()).filter(f -> f >= limitPrice).collect(Collectors.toSet());
		int tradingCount = trade(buyingPriceToQuantity, relativeBidPrices, quantity);
		
		int quantityLeft = quantity - tradingCount;

		// Only update the selling price list if there's any quantity left to add to the selling price map
		updatePriceQuantity(sellingPriceToQuantity, quantityLeft, limitPrice);

	}

	@Override
	public String getBookAsString() {
		// JSON looks like this: {bids={430.1=10, 430.0=10}, offers={}}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.createObjectNode();
		
		// Nodes which represent the bids and offers
		JsonNode bids = mapper.createObjectNode();
		JsonNode offers = mapper.createObjectNode();
		 
		// Get sorted Maps by key (price). According to the JSON output, bids is descending and offers is ascending
		Map<Double, Integer> sortedBuyingPriceToQuantity = 
				buyingPriceToQuantity.entrySet().stream()
												.sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
												.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
														(e1, e2) -> e2, LinkedHashMap::new));
		
		Map<Double, Integer> sortedSellingPriceToQuantity = 
				sellingPriceToQuantity.entrySet().stream()
												.sorted(Map.Entry.comparingByKey())
												.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
														(e1, e2) -> e2, LinkedHashMap::new));
		
			
		// Grab all of the bids and building the "bid" portion
		for (Map.Entry<Double, Integer> entry : sortedBuyingPriceToQuantity.entrySet()) {
			((ObjectNode) bids).put(entry.getKey().toString(), entry.getValue().toString());
		}
		
		((ObjectNode) rootNode).set("bids", bids);
		
		// Grab all of the offers and building the "offers" portion
		for (Map.Entry<Double, Integer> entry : sortedSellingPriceToQuantity.entrySet()) {
			((ObjectNode) offers).put(entry.getKey().toString(), entry.getValue());
		}
		
		((ObjectNode) rootNode).set("offers", offers);
		
		// Replace the : in the JSON with =
		// Remove the "" marks
		// Put a space after each comma
		String result = rootNode.toString().replace(":", "=").replace("\"", "").replace(",", ", ");
		
		System.out.println(result);
		
		return result;
	}
	
	// Used to update the Price Quantity
	public void updatePriceQuantity(Map<Double, Integer> priceToQuantity, Integer quantityLeft, Double limitPrice) {
		
		// Only update the price list if there's any quantity left to add to the price map
		if (quantityLeft != 0) {
			// If it does not exist...
			if (!priceToQuantity.containsKey(limitPrice) && quantityLeft != 0) {
				// Insert it into the map
				priceToQuantity.put(limitPrice, quantityLeft);
			}
			// Otherwise
			else {
				// Update the current quantity with the summation of the current + quantity
				priceToQuantity.put(limitPrice, priceToQuantity.get(limitPrice) + quantityLeft);
			}
		}
		
	}
	
	public int trade(Map<Double, Integer> priceToQuantity, Set<Double> relevantPrices, int quantity) {
		int tradingCount = 0;
		
		// If you are buying and there are offers <= to your bid, reduce
		// the quantity starting from the lowest price offered up until
		// the bid you are offering
		if (!relevantPrices.isEmpty()) {
			
			// Loop through the prices...
			for (Double offerPrice : relevantPrices) {
				// While the selling price map contains the price and did not reach quantity
				while (tradingCount != quantity && priceToQuantity.containsKey(offerPrice)) {
					// Reduce the quantity in the map
					priceToQuantity.put(offerPrice, priceToQuantity.get(offerPrice) - 1);
					
					tradingCount++;
				
					// Removes the price if the quantity is 0
					if (priceToQuantity.get(offerPrice) == 0) {
						priceToQuantity.remove(offerPrice);
					}
				}
				
				// Stop loop if we reached the quantity  
				if (tradingCount == quantity) {
					break;
				}
			}
		}
		
		return tradingCount;
	}

}
