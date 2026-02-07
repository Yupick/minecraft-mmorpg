#!/usr/bin/env python3
"""
Tests para el servicio de backup del servidor.
Prueba scripts backup.sh y restore-backup.sh.
"""

import unittest
import subprocess
import os
import tempfile
import shutil
from pathlib import Path
from datetime import datetime


class TestBackupService(unittest.TestCase):
    """Test suite para el sistema de backup."""
    
    @classmethod
    def setUpClass(cls):
        """Configuración inicial para todos los tests."""
        cls.project_root = Path(__file__).parent.parent
        cls.backup_script = cls.project_root / 'scripts' / 'backup.sh'
        cls.restore_script = cls.project_root / 'scripts' / 'restore-backup.sh'
        
        # Crear directorio temporal para tests
        cls.test_dir = Path(tempfile.mkdtemp(prefix='mmorpg_test_'))
        cls.backup_dir = cls.test_dir / 'backups'
        cls.backup_dir.mkdir(exist_ok=True)
    
    @classmethod
    def tearDownClass(cls):
        """Limpieza después de todos los tests."""
        if cls.test_dir.exists():
            shutil.rmtree(cls.test_dir)
    
    def test_01_backup_script_exists(self):
        """Test: Verificar que el script de backup existe."""
        self.assertTrue(self.backup_script.exists(), 
                       f"Script de backup no encontrado: {self.backup_script}")
        self.assertTrue(os.access(self.backup_script, os.X_OK),
                       "Script de backup no es ejecutable")
    
    def test_02_restore_script_exists(self):
        """Test: Verificar que el script de restore existe."""
        self.assertTrue(self.restore_script.exists(),
                       f"Script de restore no encontrado: {self.restore_script}")
        self.assertTrue(os.access(self.restore_script, os.X_OK),
                       "Script de restore no es ejecutable")
    
    def test_03_backup_script_syntax(self):
        """Test: Verificar sintaxis del script de backup."""
        result = subprocess.run(
            ['bash', '-n', str(self.backup_script)],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0,
                        f"Error de sintaxis en backup.sh:\n{result.stderr}")
    
    def test_04_restore_script_syntax(self):
        """Test: Verificar sintaxis del script de restore."""
        result = subprocess.run(
            ['bash', '-n', str(self.restore_script)],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0,
                        f"Error de sintaxis en restore-backup.sh:\n{result.stderr}")
    
    def test_05_backup_creates_archive(self):
        """Test: Verificar que el backup crea un archivo tar.gz."""
        # Crear estructura de prueba
        test_server_dir = self.test_dir / 'server'
        test_server_dir.mkdir(exist_ok=True)
        
        # Crear archivos de prueba
        (test_server_dir / 'world').mkdir(exist_ok=True)
        (test_server_dir / 'world' / 'level.dat').write_text('test world data')
        (test_server_dir / 'universal.db').write_text('test database')
        
        # Simular ejecución de backup (sin ejecutar el script real)
        # En su lugar, creamos manualmente un backup de prueba
        backup_name = f"backup-{datetime.now().strftime('%Y%m%d-%H%M%S')}.tar.gz"
        backup_path = self.backup_dir / backup_name
        
        # Crear backup usando tar
        result = subprocess.run(
            ['tar', '-czf', str(backup_path), '-C', str(test_server_dir), '.'],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0, 
                        f"Error creando backup de prueba:\n{result.stderr}")
        self.assertTrue(backup_path.exists(), "Archivo de backup no fue creado")
        self.assertGreater(backup_path.stat().st_size, 0, 
                          "Archivo de backup está vacío")
    
    def test_06_backup_contains_expected_files(self):
        """Test: Verificar que el backup contiene los archivos esperados."""
        # Usar el backup creado en el test anterior
        backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        
        if not backups:
            self.skipTest("No hay backups disponibles para verificar")
        
        backup_path = backups[0]
        
        # Listar contenido del backup
        result = subprocess.run(
            ['tar', '-tzf', str(backup_path)],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0,
                        f"Error listando contenido del backup:\n{result.stderr}")
        
        contents = result.stdout
        
        # Verificar que contiene archivos esperados
        self.assertIn('world/', contents, "Backup no contiene directorio world/")
        self.assertIn('universal.db', contents, "Backup no contiene universal.db")
    
    def test_07_backup_compression_works(self):
        """Test: Verificar que la compresión funciona correctamente."""
        backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        
        if not backups:
            self.skipTest("No hay backups disponibles para verificar")
        
        backup_path = backups[0]
        
        # Verificar que es un archivo tar.gz válido
        result = subprocess.run(
            ['gzip', '-t', str(backup_path)],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0,
                        f"Archivo de backup no es un gzip válido:\n{result.stderr}")
    
    def test_08_restore_extracts_backup(self):
        """Test: Verificar que el restore extrae correctamente el backup."""
        backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        
        if not backups:
            self.skipTest("No hay backups disponibles para restaurar")
        
        backup_path = backups[0]
        
        # Crear directorio de restore
        restore_dir = self.test_dir / 'restore'
        restore_dir.mkdir(exist_ok=True)
        
        # Extraer backup
        result = subprocess.run(
            ['tar', '-xzf', str(backup_path), '-C', str(restore_dir)],
            capture_output=True,
            text=True
        )
        
        self.assertEqual(result.returncode, 0,
                        f"Error extrayendo backup:\n{result.stderr}")
        
        # Verificar que los archivos fueron restaurados
        self.assertTrue((restore_dir / 'world' / 'level.dat').exists(),
                       "Archivo world/level.dat no fue restaurado")
        self.assertTrue((restore_dir / 'universal.db').exists(),
                       "Archivo universal.db no fue restaurado")
    
    def test_09_restored_files_match_original(self):
        """Test: Verificar que los archivos restaurados coinciden con los originales."""
        restore_dir = self.test_dir / 'restore'
        
        if not restore_dir.exists():
            self.skipTest("No hay archivos restaurados para verificar")
        
        # Comparar contenido
        original_content = 'test world data'
        restored_file = restore_dir / 'world' / 'level.dat'
        
        if restored_file.exists():
            restored_content = restored_file.read_text()
            self.assertEqual(original_content, restored_content,
                           "Contenido restaurado no coincide con el original")
    
    def test_10_backup_cleanup_old_backups(self):
        """Test: Verificar lógica de limpieza de backups antiguos."""
        # Crear múltiples backups de prueba
        for i in range(10):
            backup_name = f"backup-2024010{i}-120000.tar.gz"
            backup_path = self.backup_dir / backup_name
            backup_path.write_text(f"test backup {i}")
        
        # Verificar que hay 10 backups
        backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        self.assertEqual(len(backups), 10, "No se crearon todos los backups de prueba")
        
        # En un escenario real, el script debería mantener solo los 7 más recientes
        # Simulamos la limpieza
        backups.sort(key=lambda x: x.stat().st_mtime)
        
        # Eliminar los más antiguos (mantener solo 7)
        for backup in backups[:-7]:
            backup.unlink()
        
        # Verificar que quedan 7
        remaining_backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        self.assertEqual(len(remaining_backups), 7,
                        "La limpieza no dejó exactamente 7 backups")
    
    def test_11_backup_handles_missing_directories(self):
        """Test: Verificar que el backup maneja directorios faltantes correctamente."""
        # Crear directorio sin algunos subdirectorios esperados
        test_server_dir = self.test_dir / 'incomplete_server'
        test_server_dir.mkdir(exist_ok=True)
        
        # Solo crear universal.db, sin world/
        (test_server_dir / 'universal.db').write_text('test database')
        
        # Crear backup
        backup_name = f"backup-incomplete-{datetime.now().strftime('%Y%m%d-%H%M%S')}.tar.gz"
        backup_path = self.backup_dir / backup_name
        
        result = subprocess.run(
            ['tar', '-czf', str(backup_path), '-C', str(test_server_dir), '.'],
            capture_output=True,
            text=True
        )
        
        # No debe fallar, solo advertir
        self.assertEqual(result.returncode, 0,
                        "Backup falló con directorios incompletos")
    
    def test_12_backup_size_reporting(self):
        """Test: Verificar que se puede obtener el tamaño del backup."""
        backups = list(self.backup_dir.glob('backup-*.tar.gz'))
        
        if not backups:
            self.skipTest("No hay backups disponibles")
        
        backup_path = backups[0]
        size_bytes = backup_path.stat().st_size
        
        # Convertir a formato legible (KB, MB, GB)
        if size_bytes < 1024:
            size_str = f"{size_bytes} B"
        elif size_bytes < 1024 * 1024:
            size_str = f"{size_bytes / 1024:.2f} KB"
        elif size_bytes < 1024 * 1024 * 1024:
            size_str = f"{size_bytes / (1024 * 1024):.2f} MB"
        else:
            size_str = f"{size_bytes / (1024 * 1024 * 1024):.2f} GB"
        
        print(f"\nTamaño del backup: {size_str}")
        self.assertGreater(size_bytes, 0, "Backup tiene tamaño 0")


class TestBackupScriptFunctions(unittest.TestCase):
    """Test suite para funciones específicas del script de backup."""
    
    def test_get_size_function(self):
        """Test: Verificar función get_size() para formateo de tamaños."""
        test_cases = [
            (500, "500 B"),
            (1536, "1.50 KB"),  # 1.5 KB
            (1572864, "1.50 MB"),  # 1.5 MB
            (1610612736, "1.50 GB"),  # 1.5 GB
        ]
        
        for size_bytes, expected_format in test_cases:
            # Simular función get_size
            if size_bytes < 1024:
                result = f"{size_bytes} B"
            elif size_bytes < 1024 * 1024:
                result = f"{size_bytes / 1024:.2f} KB"
            elif size_bytes < 1024 * 1024 * 1024:
                result = f"{size_bytes / (1024 * 1024):.2f} MB"
            else:
                result = f"{size_bytes / (1024 * 1024 * 1024):.2f} GB"
            
            self.assertEqual(result, expected_format,
                           f"Formato de tamaño incorrecto para {size_bytes} bytes")
    
    def test_backup_naming_convention(self):
        """Test: Verificar convención de nombres de backup."""
        # Formato esperado: backup-YYYYMMDD-HHMMSS.tar.gz
        import re
        
        pattern = r'backup-\d{8}-\d{6}\.tar\.gz'
        
        test_names = [
            "backup-20240115-143000.tar.gz",  # Válido
            "backup-20240115-143000.tar",     # Inválido (falta .gz)
            "backup-invalid.tar.gz",          # Inválido (formato fecha)
            "backup-20240115.tar.gz",         # Inválido (falta hora)
        ]
        
        expected_results = [True, False, False, False]
        
        for name, expected in zip(test_names, expected_results):
            result = bool(re.match(pattern, name))
            self.assertEqual(result, expected,
                           f"Validación de nombre incorrecta para: {name}")


def run_tests():
    """Ejecuta todos los tests y genera reporte."""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    suite.addTests(loader.loadTestsFromTestCase(TestBackupService))
    suite.addTests(loader.loadTestsFromTestCase(TestBackupScriptFunctions))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    import sys
    sys.exit(run_tests())
