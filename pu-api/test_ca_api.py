from ca_catalogue_api import CACatalogueAPI

api = CACatalogueAPI("ipos_ca.db")

# Get all items
items = api.get_catalogue_items()
print(f"Total items: {len(items)}")
for item in items:
    print(f"  {item['item_id']} - {item['description']} - £{item['cost_per_unit']} per {item['unit']}")

# Search
results = api.search_catalogue_items("para")
print(f"\nSearch 'para': {len(results)} results")
for r in results:
    print(f"  {r['item_id']} - {r['description']}")

# Get by ID
if items:
    single = api.get_catalogue_item_by_id(items[0]['item_id'])
    print(f"\nSingle lookup: {single}")
