import sqlite3

class CACatalogueAPI:
    """
    CA to PU Catalogue API
    Reads from CA's local SQLite database (ipos_ca.db).
    """

    def __init__(self, db_path):
        """
        :param db_path: path to ipos_ca.db file
        """
        self.db_path = db_path

    def _connect(self):
        return sqlite3.connect(self.db_path)

    def get_catalogue_items(self):
        """Returns all catalogue items as a list of dicts."""
        conn = self._connect()
        conn.row_factory = sqlite3.Row
        cursor = conn.execute(
            "SELECT item_id, description, package_type, unit, "
            "units_per_pack, cost_per_unit, availability FROM sa_catalogue"
        )
        items = [dict(row) for row in cursor.fetchall()]
        conn.close()
        return items

    def search_catalogue_items(self, keyword):
        """Searches catalogue by keyword (matches description)."""
        if not keyword or not keyword.strip():
            return self.get_catalogue_items()
        conn = self._connect()
        conn.row_factory = sqlite3.Row
        cursor = conn.execute(
            "SELECT item_id, description, package_type, unit, "
            "units_per_pack, cost_per_unit, availability FROM sa_catalogue "
            "WHERE description LIKE ?",
            (f"%{keyword.strip()}%",)
        )
        items = [dict(row) for row in cursor.fetchall()]
        conn.close()
        return items

    def get_catalogue_item_by_id(self, item_id):
        """Returns a single item by ID, or None if not found."""
        conn = self._connect()
        conn.row_factory = sqlite3.Row
        cursor = conn.execute(
            "SELECT item_id, description, package_type, unit, "
            "units_per_pack, cost_per_unit, availability FROM sa_catalogue "
            "WHERE item_id = ?",
            (item_id,)
        )
        row = cursor.fetchone()
        conn.close()
        return dict(row) if row else None
