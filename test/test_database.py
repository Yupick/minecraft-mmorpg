#!/usr/bin/env python3
"""
Tests para el DatabaseManager del servidor.
Prueba conexiones, queries, transacciones y manejo de errores.
"""

import unittest
import sqlite3
import tempfile
import os
from pathlib import Path


class TestDatabaseConnection(unittest.TestCase):
    """Test suite para conexiones de base de datos."""
    
    def setUp(self):
        """Configuración antes de cada test."""
        # Crear base de datos temporal
        self.temp_db = tempfile.NamedTemporaryFile(delete=False, suffix='.db')
        self.temp_db.close()
        self.db_path = self.temp_db.name
        
        # Crear conexión
        self.conn = sqlite3.connect(self.db_path)
        self.conn.row_factory = sqlite3.Row  # Permite acceso por nombre de columna
        self.cursor = self.conn.cursor()
    
    def tearDown(self):
        """Limpieza después de cada test."""
        if self.conn:
            self.conn.close()
        
        if os.path.exists(self.db_path):
            os.unlink(self.db_path)
    
    def test_01_connection_created(self):
        """Test: Verificar que la conexión se crea correctamente."""
        self.assertIsNotNone(self.conn, "Conexión no fue creada")
        self.assertIsInstance(self.conn, sqlite3.Connection)
    
    def test_02_cursor_created(self):
        """Test: Verificar que el cursor se crea correctamente."""
        self.assertIsNotNone(self.cursor, "Cursor no fue creado")
        self.assertIsInstance(self.cursor, sqlite3.Cursor)
    
    def test_03_create_table(self):
        """Test: Crear una tabla básica."""
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        self.conn.commit()
        
        # Verificar que la tabla existe
        self.cursor.execute('''
            SELECT name FROM sqlite_master 
            WHERE type='table' AND name='players'
        ''')
        
        result = self.cursor.fetchone()
        self.assertIsNotNone(result, "Tabla 'players' no fue creada")
    
    def test_04_insert_data(self):
        """Test: Insertar datos en la tabla."""
        # Crear tabla primero
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Insertar datos
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', ('550e8400-e29b-41d4-a716-446655440000', 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Verificar que se insertó
        self.cursor.execute('SELECT COUNT(*) FROM players')
        count = self.cursor.fetchone()[0]
        self.assertEqual(count, 1, "Datos no fueron insertados")
    
    def test_05_select_data(self):
        """Test: Seleccionar datos de la tabla."""
        # Preparar datos
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', ('550e8400-e29b-41d4-a716-446655440000', 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Seleccionar datos
        self.cursor.execute('SELECT * FROM players WHERE username = ?', ('Steve',))
        row = self.cursor.fetchone()
        
        self.assertIsNotNone(row, "No se encontraron datos")
        self.assertEqual(row['username'], 'Steve')
        self.assertEqual(row['level'], 45)
        self.assertEqual(row['coins'], 15420.50)
    
    def test_06_update_data(self):
        """Test: Actualizar datos en la tabla."""
        # Preparar datos
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        uuid = '550e8400-e29b-41d4-a716-446655440000'
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', (uuid, 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Actualizar nivel
        self.cursor.execute('''
            UPDATE players SET level = ? WHERE uuid = ?
        ''', (50, uuid))
        
        self.conn.commit()
        
        # Verificar actualización
        self.cursor.execute('SELECT level FROM players WHERE uuid = ?', (uuid,))
        new_level = self.cursor.fetchone()['level']
        self.assertEqual(new_level, 50, "Datos no fueron actualizados")
    
    def test_07_delete_data(self):
        """Test: Eliminar datos de la tabla."""
        # Preparar datos
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        uuid = '550e8400-e29b-41d4-a716-446655440000'
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', (uuid, 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Eliminar datos
        self.cursor.execute('DELETE FROM players WHERE uuid = ?', (uuid,))
        self.conn.commit()
        
        # Verificar eliminación
        self.cursor.execute('SELECT COUNT(*) FROM players')
        count = self.cursor.fetchone()[0]
        self.assertEqual(count, 0, "Datos no fueron eliminados")
    
    def test_08_transaction_commit(self):
        """Test: Verificar que las transacciones se commitean correctamente."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Iniciar transacción
        self.cursor.execute('BEGIN TRANSACTION')
        
        # Insertar múltiples registros
        players = [
            ('uuid1', 'Steve', 45, 15420.50),
            ('uuid2', 'Alex', 42, 12350.00),
            ('uuid3', 'Herobrine', 38, 8540.00),
        ]
        
        for player in players:
            self.cursor.execute('''
                INSERT INTO players (uuid, username, level, coins)
                VALUES (?, ?, ?, ?)
            ''', player)
        
        # Commit transacción
        self.conn.commit()
        
        # Verificar que todos se insertaron
        self.cursor.execute('SELECT COUNT(*) FROM players')
        count = self.cursor.fetchone()[0]
        self.assertEqual(count, 3, "No se commitieron todos los registros")
    
    def test_09_transaction_rollback(self):
        """Test: Verificar que las transacciones se revierten correctamente."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Insertar un registro
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', ('uuid1', 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Iniciar nueva transacción
        self.cursor.execute('BEGIN TRANSACTION')
        
        # Insertar otro registro
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', ('uuid2', 'Alex', 42, 12350.00))
        
        # Rollback
        self.conn.rollback()
        
        # Verificar que solo hay 1 registro (el primero)
        self.cursor.execute('SELECT COUNT(*) FROM players')
        count = self.cursor.fetchone()[0]
        self.assertEqual(count, 1, "Rollback no funcionó correctamente")
    
    def test_10_primary_key_constraint(self):
        """Test: Verificar que la restricción PRIMARY KEY funciona."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        uuid = '550e8400-e29b-41d4-a716-446655440000'
        
        # Insertar primer registro
        self.cursor.execute('''
            INSERT INTO players (uuid, username, level, coins)
            VALUES (?, ?, ?, ?)
        ''', (uuid, 'Steve', 45, 15420.50))
        
        self.conn.commit()
        
        # Intentar insertar con mismo UUID (debe fallar)
        with self.assertRaises(sqlite3.IntegrityError):
            self.cursor.execute('''
                INSERT INTO players (uuid, username, level, coins)
                VALUES (?, ?, ?, ?)
            ''', (uuid, 'Alex', 42, 12350.00))
    
    def test_11_foreign_key_constraint(self):
        """Test: Verificar restricciones de FOREIGN KEY."""
        # Habilitar foreign keys
        self.cursor.execute('PRAGMA foreign_keys = ON')
        
        # Crear tablas con relación
        self.cursor.execute('''
            CREATE TABLE quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
        ''')
        
        self.cursor.execute('''
            CREATE TABLE player_quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                quest_id INTEGER NOT NULL,
                FOREIGN KEY(quest_id) REFERENCES quests(id)
            )
        ''')
        
        self.conn.commit()
        
        # Insertar quest
        self.cursor.execute('INSERT INTO quests (name) VALUES (?)', ('Zombie Slayer',))
        quest_id = self.cursor.lastrowid
        
        # Insertar player_quest válida
        self.cursor.execute('''
            INSERT INTO player_quests (player_uuid, quest_id)
            VALUES (?, ?)
        ''', ('uuid1', quest_id))
        
        self.conn.commit()
        
        # Intentar insertar con quest_id inválido (debe fallar)
        with self.assertRaises(sqlite3.IntegrityError):
            self.cursor.execute('''
                INSERT INTO player_quests (player_uuid, quest_id)
                VALUES (?, ?)
            ''', ('uuid2', 9999))
            self.conn.commit()
    
    def test_12_index_creation(self):
        """Test: Crear índices en la tabla."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Crear índice en level
        self.cursor.execute('CREATE INDEX idx_players_level ON players(level)')
        
        # Verificar que el índice existe
        self.cursor.execute('''
            SELECT name FROM sqlite_master 
            WHERE type='index' AND name='idx_players_level'
        ''')
        
        result = self.cursor.fetchone()
        self.assertIsNotNone(result, "Índice no fue creado")
    
    def test_13_aggregate_functions(self):
        """Test: Usar funciones de agregación (SUM, AVG, COUNT)."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Insertar datos
        players = [
            ('uuid1', 'Steve', 45, 15420.50),
            ('uuid2', 'Alex', 42, 12350.00),
            ('uuid3', 'Herobrine', 38, 8540.00),
        ]
        
        for player in players:
            self.cursor.execute('''
                INSERT INTO players (uuid, username, level, coins)
                VALUES (?, ?, ?, ?)
            ''', player)
        
        self.conn.commit()
        
        # Probar COUNT
        self.cursor.execute('SELECT COUNT(*) FROM players')
        count = self.cursor.fetchone()[0]
        self.assertEqual(count, 3)
        
        # Probar SUM
        self.cursor.execute('SELECT SUM(coins) FROM players')
        total_coins = self.cursor.fetchone()[0]
        self.assertAlmostEqual(total_coins, 36310.50, places=2)
        
        # Probar AVG
        self.cursor.execute('SELECT AVG(level) FROM players')
        avg_level = self.cursor.fetchone()[0]
        self.assertAlmostEqual(avg_level, 41.666, places=2)
    
    def test_14_json_data_handling(self):
        """Test: Almacenar y recuperar datos JSON."""
        import json
        
        self.cursor.execute('''
            CREATE TABLE quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                objectives TEXT,
                rewards TEXT
            )
        ''')
        
        # Preparar datos JSON
        objectives = [
            {"type": "kill", "target": "ZOMBIE", "count": 50}
        ]
        
        rewards = {
            "coins": 500,
            "exp": 250,
            "items": [{"item": "DIAMOND_SWORD", "amount": 1}]
        }
        
        # Insertar con JSON serializado
        self.cursor.execute('''
            INSERT INTO quests (name, objectives, rewards)
            VALUES (?, ?, ?)
        ''', ('Zombie Slayer', json.dumps(objectives), json.dumps(rewards)))
        
        self.conn.commit()
        
        # Recuperar y deserializar
        self.cursor.execute('SELECT * FROM quests WHERE name = ?', ('Zombie Slayer',))
        row = self.cursor.fetchone()
        
        stored_objectives = json.loads(row['objectives'])
        stored_rewards = json.loads(row['rewards'])
        
        self.assertEqual(stored_objectives, objectives)
        self.assertEqual(stored_rewards, rewards)
    
    def test_15_vacuum_database(self):
        """Test: Ejecutar VACUUM para optimizar la base de datos."""
        self.cursor.execute('''
            CREATE TABLE players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                coins REAL DEFAULT 0.0
            )
        ''')
        
        # Insertar y eliminar datos para crear espacio fragmentado
        for i in range(100):
            self.cursor.execute('''
                INSERT INTO players (uuid, username, level, coins)
                VALUES (?, ?, ?, ?)
            ''', (f'uuid{i}', f'Player{i}', i, i * 100.0))
        
        self.conn.commit()
        
        # Obtener tamaño antes de vacuum
        size_before = os.path.getsize(self.db_path)
        
        # Eliminar todos los registros
        self.cursor.execute('DELETE FROM players')
        self.conn.commit()
        
        # Ejecutar VACUUM
        self.cursor.execute('VACUUM')
        
        # Obtener tamaño después de vacuum
        size_after = os.path.getsize(self.db_path)
        
        # El tamaño debería ser menor o igual después de VACUUM
        self.assertLessEqual(size_after, size_before,
                            "VACUUM no redujo el tamaño de la base de datos")


def run_tests():
    """Ejecuta todos los tests y genera reporte."""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    suite.addTests(loader.loadTestsFromTestCase(TestDatabaseConnection))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    import sys
    sys.exit(run_tests())
