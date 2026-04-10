package API;

import java.util.List;
import java.util.Map;

/**
 * IPOS-CA API for IPOS-PU (Public User / Online Portal).
 *
 * Provides Cosymed's retail catalogue. All prices are retail
 * (markup + VAT applied). No wholesale costs are exposed.
 *
 * Catalogue item keys:
 *   - "itemId"       (String)  e.g. "100 00001"
 *   - "description"  (String)  e.g. "Paracetamol"
 *   - "quantity"     (String)  packs in stock, e.g. "150"
 *   - "unitCost"     (String)  retail price per pack, e.g. "0.15"
 *   - "totalCost"    (String)  unitCost * quantity, e.g. "22.50"
 */
public interface CA_PU_interface {

    /**
     * Returns the full retail catalogue.
     * Only items with availability > 0 are included.
     */
    List<Map<String, String>> getCatalogue() throws Exception;

    /**
     * Searches the retail catalogue by keyword (matches item ID or description).
     * Only items with availability > 0 are included.
     */
    List<Map<String, String>> searchCatalogue(String keyword) throws Exception;

    /**
     * Returns details for a single item by its ID.
     * Returns null if the item does not exist.
     */
    Map<String, String> getItemDetails(String itemId) throws Exception;

    /**
     * Checks whether the requested quantity is available for an item.
     * Returns true if the item exists and has enough stock.
     */
    boolean checkAvailability(String itemId, int quantity) throws Exception;

    /**
     * Returns the current VAT rate as a percentage (e.g. "20.0" for 20%).
     */
    String getVATRate() throws Exception;
}
