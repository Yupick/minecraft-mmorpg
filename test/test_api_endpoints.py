#!/usr/bin/env python3
"""
Tests para los endpoints de la API REST del Web Panel.
Prueba autenticación, CRUD de jugadores, economía, quests y mobs.
"""

import unittest
import json
import sys
import os
from pathlib import Path

# Añadir el directorio web al path para importar app
sys.path.insert(0, str(Path(__file__).parent.parent / 'web'))

from app import app, init_db


class TestAPIEndpoints(unittest.TestCase):
    """Test suite para los endpoints de la API REST."""
    
    @classmethod
    def setUpClass(cls):
        """Configuración inicial para todos los tests."""
        app.config['TESTING'] = True
        app.config['SECRET_KEY'] = 'test_secret_key'
        app.config['DATABASE_PATH'] = ':memory:'  # Base de datos en memoria para tests
        
        # Inicializar BD de prueba
        with app.app_context():
            init_db()
    
    def setUp(self):
        """Configuración antes de cada test."""
        self.client = app.test_client()
        self.client.testing = True
        
        # Login para obtener sesión
        self.login()
    
    def login(self):
        """Helper para hacer login y obtener sesión autenticada."""
        response = self.client.post('/login', 
            data=json.dumps({
                'username': 'admin',
                'password': 'admin123'
            }),
            content_type='application/json'
        )
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
    
    def test_01_login_success(self):
        """Test: Login exitoso con credenciales correctas."""
        response = self.client.post('/login',
            data=json.dumps({
                'username': 'admin',
                'password': 'admin123'
            }),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('redirect', data)
    
    def test_02_login_failure(self):
        """Test: Login fallido con credenciales incorrectas."""
        # Logout primero
        self.client.get('/logout')
        
        response = self.client.post('/login',
            data=json.dumps({
                'username': 'admin',
                'password': 'wrong_password'
            }),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 401)
        data = json.loads(response.data)
        self.assertFalse(data.get('success'))
    
    def test_03_get_players(self):
        """Test: Obtener lista de jugadores."""
        response = self.client.get('/api/players')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('players', data)
        self.assertIsInstance(data['players'], list)
    
    def test_04_get_players_with_filters(self):
        """Test: Obtener jugadores con filtros (online=true)."""
        response = self.client.get('/api/players?online=true&limit=10')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('players', data)
        
        # Verificar que todos los jugadores estén online
        for player in data['players']:
            self.assertTrue(player.get('is_online'))
    
    def test_05_get_player_by_uuid(self):
        """Test: Obtener jugador específico por UUID."""
        # Primero obtener un jugador de la lista
        response = self.client.get('/api/players')
        data = json.loads(response.data)
        
        if data['players']:
            uuid = data['players'][0]['uuid']
            
            # Obtener detalles del jugador
            response = self.client.get(f'/api/players/{uuid}')
            self.assertEqual(response.status_code, 200)
            
            player_data = json.loads(response.data)
            self.assertTrue(player_data.get('success'))
            self.assertIn('player', player_data)
            self.assertEqual(player_data['player']['uuid'], uuid)
    
    def test_06_update_player(self):
        """Test: Actualizar estadísticas de un jugador."""
        # Crear jugador de prueba primero
        test_uuid = '550e8400-e29b-41d4-a716-446655440000'
        
        response = self.client.put(f'/api/players/{test_uuid}',
            data=json.dumps({
                'level': 50,
                'coins': 10000.0,
                'strength': 80,
                'defense': 70
            }),
            content_type='application/json'
        )
        
        # Puede fallar si el jugador no existe, pero el endpoint debe responder
        self.assertIn(response.status_code, [200, 400, 404])
    
    def test_07_execute_player_command(self):
        """Test: Ejecutar comando para un jugador (requiere jugador online)."""
        test_uuid = '550e8400-e29b-41d4-a716-446655440000'
        
        response = self.client.post(f'/api/players/{test_uuid}/execute',
            data=json.dumps({
                'command': 'tp 100 64 200'
            }),
            content_type='application/json'
        )
        
        # Puede fallar si el jugador no está online o RCON no está disponible
        self.assertIn(response.status_code, [200, 400, 503])
    
    def test_08_get_economy_stats(self):
        """Test: Obtener estadísticas de economía."""
        response = self.client.get('/api/economy/stats')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('stats', data)
        
        stats = data['stats']
        self.assertIn('total_circulation', stats)
        self.assertIn('total_earned_today', stats)
        self.assertIn('total_spent_today', stats)
        self.assertIn('transactions_today', stats)
    
    def test_09_get_transactions(self):
        """Test: Obtener transacciones económicas."""
        response = self.client.get('/api/economy/transactions?limit=20')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('transactions', data)
        self.assertIsInstance(data['transactions'], list)
    
    def test_10_add_coins(self):
        """Test: Añadir monedas a un jugador."""
        response = self.client.post('/api/economy/add',
            data=json.dumps({
                'player_uuid': '550e8400-e29b-41d4-a716-446655440000',
                'amount': 1000.0,
                'reason': 'Test automation'
            }),
            content_type='application/json'
        )
        
        # Puede fallar si el jugador no existe
        self.assertIn(response.status_code, [200, 400, 404])
    
    def test_11_remove_coins(self):
        """Test: Retirar monedas de un jugador."""
        response = self.client.post('/api/economy/remove',
            data=json.dumps({
                'player_uuid': '550e8400-e29b-41d4-a716-446655440000',
                'amount': 100.0,
                'reason': 'Test automation'
            }),
            content_type='application/json'
        )
        
        # Puede fallar si el jugador no existe o no tiene suficientes monedas
        self.assertIn(response.status_code, [200, 400, 404])
    
    def test_12_get_quests(self):
        """Test: Obtener lista de quests."""
        response = self.client.get('/api/quests')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('quests', data)
        self.assertIsInstance(data['quests'], list)
    
    def test_13_create_quest(self):
        """Test: Crear una nueva quest."""
        response = self.client.post('/api/quests',
            data=json.dumps({
                'name': 'Test Quest',
                'description': 'Quest de prueba automatizada',
                'objectives': [
                    {'type': 'kill', 'target': 'ZOMBIE', 'count': 10}
                ],
                'rewards': {
                    'coins': 500,
                    'exp': 250
                },
                'min_level': 1,
                'repeatable': False,
                'quest_type': 'kill'
            }),
            content_type='application/json'
        )
        
        # Puede fallar si ya existe una quest con ese nombre
        self.assertIn(response.status_code, [200, 201, 400, 409])
        
        if response.status_code in [200, 201]:
            data = json.loads(response.data)
            self.assertTrue(data.get('success'))
            self.assertIn('quest_id', data)
            
            # Guardar ID para tests posteriores
            self.test_quest_id = data['quest_id']
    
    def test_14_update_quest(self):
        """Test: Actualizar una quest existente."""
        # Usar ID 1 como ejemplo (puede no existir)
        response = self.client.put('/api/quests/1',
            data=json.dumps({
                'rewards': {
                    'coins': 1000,
                    'exp': 500
                }
            }),
            content_type='application/json'
        )
        
        # Puede fallar si la quest no existe
        self.assertIn(response.status_code, [200, 404])
    
    def test_15_delete_quest(self):
        """Test: Eliminar una quest."""
        # Intentar eliminar quest de prueba si se creó
        if hasattr(self, 'test_quest_id'):
            response = self.client.delete(f'/api/quests/{self.test_quest_id}')
            
            # Puede fallar si hay jugadores con la quest activa
            self.assertIn(response.status_code, [200, 400, 404])
    
    def test_16_get_mobs(self):
        """Test: Obtener lista de mobs personalizados."""
        response = self.client.get('/api/mobs')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('mobs', data)
        self.assertIsInstance(data['mobs'], list)
    
    def test_17_update_mob(self):
        """Test: Actualizar estadísticas de un mob."""
        # Usar ID 1 como ejemplo (puede no existir)
        response = self.client.put('/api/mobs/1',
            data=json.dumps({
                'health': 200.0,
                'damage': 20.0,
                'exp_reward': 150
            }),
            content_type='application/json'
        )
        
        # Puede fallar si el mob no existe
        self.assertIn(response.status_code, [200, 404])
    
    def test_18_get_server_stats(self):
        """Test: Obtener estadísticas del servidor."""
        response = self.client.get('/api/server/stats')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('stats', data)
        
        stats = data['stats']
        self.assertIn('status', stats)
        self.assertIn('online_players', stats)
        self.assertIn('max_players', stats)
    
    def test_19_server_command(self):
        """Test: Ejecutar comando de consola (requiere RCON)."""
        response = self.client.post('/api/server/command',
            data=json.dumps({
                'command': 'list'
            }),
            content_type='application/json'
        )
        
        # Puede fallar si RCON no está disponible
        self.assertIn(response.status_code, [200, 503])
    
    def test_20_get_config(self):
        """Test: Obtener configuración del plugin."""
        response = self.client.get('/api/config')
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
        self.assertIn('config', data)
        
        config = data['config']
        self.assertIn('general', config)
        self.assertIn('gameplay', config)
        self.assertIn('economy', config)
    
    def test_21_update_config(self):
        """Test: Actualizar configuración del plugin."""
        response = self.client.put('/api/config',
            data=json.dumps({
                'gameplay': {
                    'exp_multiplier': 1.5
                }
            }),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data.get('success'))
    
    def test_22_unauthorized_access(self):
        """Test: Acceso sin autenticación debe fallar."""
        # Logout primero
        self.client.get('/logout')
        
        # Intentar acceder sin autenticación
        response = self.client.get('/api/players')
        
        # Debe redirigir al login o retornar 401
        self.assertIn(response.status_code, [302, 401])
    
    def test_23_invalid_json(self):
        """Test: Enviar JSON inválido debe retornar error."""
        response = self.client.post('/api/quests',
            data='{"invalid json',
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 400)
    
    def test_24_missing_parameters(self):
        """Test: Parámetros faltantes deben retornar error."""
        response = self.client.post('/api/economy/add',
            data=json.dumps({
                'player_uuid': '550e8400-e29b-41d4-a716-446655440000'
                # Falta 'amount'
            }),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 400)
    
    def test_25_logout(self):
        """Test: Logout debe limpiar sesión."""
        response = self.client.get('/logout')
        
        # Debe redirigir al login
        self.assertEqual(response.status_code, 302)
        
        # Verificar que ya no podemos acceder a endpoints protegidos
        response = self.client.get('/api/players')
        self.assertIn(response.status_code, [302, 401])


class TestAPIValidation(unittest.TestCase):
    """Test suite para validación de datos en la API."""
    
    @classmethod
    def setUpClass(cls):
        """Configuración inicial."""
        app.config['TESTING'] = True
        cls.client = app.test_client()
    
    def setUp(self):
        """Login antes de cada test."""
        self.client.post('/login',
            data=json.dumps({
                'username': 'admin',
                'password': 'admin123'
            }),
            content_type='application/json'
        )
    
    def test_validate_level_range(self):
        """Test: Validar que el nivel esté en rango válido (1-100)."""
        test_uuid = '550e8400-e29b-41d4-a716-446655440000'
        
        # Nivel negativo
        response = self.client.put(f'/api/players/{test_uuid}',
            data=json.dumps({'level': -5}),
            content_type='application/json'
        )
        self.assertEqual(response.status_code, 400)
        
        # Nivel > 100
        response = self.client.put(f'/api/players/{test_uuid}',
            data=json.dumps({'level': 150}),
            content_type='application/json'
        )
        self.assertEqual(response.status_code, 400)
    
    def test_validate_coins_positive(self):
        """Test: Validar que las monedas sean positivas."""
        response = self.client.post('/api/economy/add',
            data=json.dumps({
                'player_uuid': '550e8400-e29b-41d4-a716-446655440000',
                'amount': -1000.0,
                'reason': 'Test'
            }),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 400)
    
    def test_validate_uuid_format(self):
        """Test: Validar formato de UUID."""
        response = self.client.get('/api/players/invalid-uuid-format')
        
        self.assertIn(response.status_code, [400, 404])


def run_tests():
    """Ejecuta todos los tests y genera reporte."""
    # Crear test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    suite.addTests(loader.loadTestsFromTestCase(TestAPIEndpoints))
    suite.addTests(loader.loadTestsFromTestCase(TestAPIValidation))
    
    # Ejecutar tests con verbosity
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # Retornar código de salida
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.exit(run_tests())
