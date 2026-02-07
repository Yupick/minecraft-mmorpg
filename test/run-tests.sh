#!/bin/bash
#
# Script para ejecutar todos los tests del proyecto MMORPG
# Ejecuta tests de Python (API, backup, database) y genera reporte
#

set -e  # Salir si hay errores

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directorio del proyecto
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_DIR="$PROJECT_DIR/test"
REPORT_DIR="$PROJECT_DIR/test/reports"

# Crear directorio de reportes si no existe
mkdir -p "$REPORT_DIR"

# Timestamp para reportes
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/test_report_$TIMESTAMP.txt"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  MMORPG Plugin - Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Función para imprimir headers
print_header() {
    echo ""
    echo -e "${YELLOW}>>> $1${NC}"
    echo "-----------------------------------"
}

# Función para verificar si Python está instalado
check_python() {
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}Error: Python 3 no está instalado${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Python $(python3 --version) encontrado${NC}"
}

# Función para instalar dependencias de Python
install_dependencies() {
    print_header "Instalando dependencias de Python"
    
    if [ -f "$PROJECT_DIR/web/requirements.txt" ]; then
        echo "Instalando dependencias desde requirements.txt..."
        pip3 install -q -r "$PROJECT_DIR/web/requirements.txt" || {
            echo -e "${YELLOW}Advertencia: Algunas dependencias no se pudieron instalar${NC}"
        }
        echo -e "${GREEN}✓ Dependencias instaladas${NC}"
    else
        echo -e "${YELLOW}Advertencia: requirements.txt no encontrado${NC}"
    fi
}

# Contadores de resultados
total_tests=0
passed_tests=0
failed_tests=0
skipped_tests=0

# Función para ejecutar un test Python
run_python_test() {
    local test_file=$1
    local test_name=$(basename "$test_file" .py)
    
    print_header "Ejecutando: $test_name"
    
    # Ejecutar test y capturar output
    if python3 "$test_file" 2>&1 | tee -a "$REPORT_FILE"; then
        echo -e "${GREEN}✓ $test_name: PASSED${NC}"
        ((passed_tests++))
        return 0
    else
        echo -e "${RED}✗ $test_name: FAILED${NC}"
        ((failed_tests++))
        return 1
    fi
}

# Función para ejecutar test de scripts bash
run_bash_script_test() {
    local script_path=$1
    local script_name=$(basename "$script_path")
    
    print_header "Verificando sintaxis: $script_name"
    
    if bash -n "$script_path" 2>&1 | tee -a "$REPORT_FILE"; then
        echo -e "${GREEN}✓ $script_name: Sintaxis correcta${NC}"
        ((passed_tests++))
        return 0
    else
        echo -e "${RED}✗ $script_name: Error de sintaxis${NC}"
        ((failed_tests++))
        return 1
    fi
}

# Inicio del reporte
{
    echo "========================================="
    echo "  MMORPG Plugin - Test Report"
    echo "  Fecha: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================="
    echo ""
} > "$REPORT_FILE"

# Verificar Python
check_python

# Instalar dependencias
install_dependencies

# Ejecutar tests de Python
print_header "Tests de Python"

if [ -f "$TEST_DIR/test_api_endpoints.py" ]; then
    run_python_test "$TEST_DIR/test_api_endpoints.py"
    total_tests=$((total_tests + 1))
fi

if [ -f "$TEST_DIR/test_backup_service.py" ]; then
    run_python_test "$TEST_DIR/test_backup_service.py"
    total_tests=$((total_tests + 1))
fi

if [ -f "$TEST_DIR/test_database.py" ]; then
    run_python_test "$TEST_DIR/test_database.py"
    total_tests=$((total_tests + 1))
fi

# Verificar sintaxis de scripts bash
print_header "Verificación de Scripts Bash"

SCRIPTS_DIR="$PROJECT_DIR/scripts"

if [ -d "$SCRIPTS_DIR" ]; then
    for script in "$SCRIPTS_DIR"/*.sh; do
        if [ -f "$script" ]; then
            run_bash_script_test "$script"
            total_tests=$((total_tests + 1))
        fi
    done
fi

# Generar resumen
print_header "Resumen de Tests"

{
    echo ""
    echo "========================================="
    echo "  RESUMEN"
    echo "========================================="
    echo "Total de tests: $total_tests"
    echo "Tests pasados: $passed_tests"
    echo "Tests fallidos: $failed_tests"
    echo "Tests omitidos: $skipped_tests"
    echo ""
    
    if [ $failed_tests -eq 0 ]; then
        echo "Estado: ✓ TODOS LOS TESTS PASARON"
    else
        echo "Estado: ✗ HAY TESTS FALLIDOS"
    fi
    
    echo "========================================="
} | tee -a "$REPORT_FILE"

echo ""
echo -e "${BLUE}Reporte completo guardado en:${NC}"
echo -e "  $REPORT_FILE"
echo ""

# Generar HTML report (opcional)
generate_html_report() {
    local html_file="$REPORT_DIR/test_report_$TIMESTAMP.html"
    
    cat > "$html_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>MMORPG Plugin - Test Report</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin: 30px 0;
        }
        .stat-card {
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        .stat-card h3 {
            margin: 0 0 10px 0;
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
        }
        .stat-card .value {
            font-size: 36px;
            font-weight: bold;
        }
        .total { background: #e3f2fd; color: #1976d2; }
        .passed { background: #e8f5e9; color: #4caf50; }
        .failed { background: #ffebee; color: #f44336; }
        .skipped { background: #fff3e0; color: #ff9800; }
        .status {
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            font-weight: bold;
            text-align: center;
        }
        .status.success {
            background: #4caf50;
            color: white;
        }
        .status.failure {
            background: #f44336;
            color: white;
        }
        pre {
            background: #f5f5f5;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }
        .timestamp {
            color: #999;
            font-size: 14px;
            text-align: right;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎮 MMORPG Plugin - Test Report</h1>
        <p class="timestamp">Generado: $(date '+%Y-%m-%d %H:%M:%S')</p>
        
        <div class="summary">
            <div class="stat-card total">
                <h3>Total Tests</h3>
                <div class="value">$total_tests</div>
            </div>
            <div class="stat-card passed">
                <h3>Pasados</h3>
                <div class="value">$passed_tests</div>
            </div>
            <div class="stat-card failed">
                <h3>Fallidos</h3>
                <div class="value">$failed_tests</div>
            </div>
            <div class="stat-card skipped">
                <h3>Omitidos</h3>
                <div class="value">$skipped_tests</div>
            </div>
        </div>
        
        <div class="status $([ $failed_tests -eq 0 ] && echo 'success' || echo 'failure')">
            $([ $failed_tests -eq 0 ] && echo '✓ TODOS LOS TESTS PASARON' || echo '✗ HAY TESTS FALLIDOS')
        </div>
        
        <h2>Reporte Completo</h2>
        <pre>$(cat "$REPORT_FILE")</pre>
    </div>
</body>
</html>
EOF
    
    echo -e "${BLUE}Reporte HTML generado en:${NC}"
    echo -e "  $html_file"
}

# Generar reporte HTML
generate_html_report

# Calcular porcentaje de éxito
if [ $total_tests -gt 0 ]; then
    success_rate=$((passed_tests * 100 / total_tests))
    echo ""
    echo -e "${BLUE}Tasa de éxito: ${GREEN}${success_rate}%${NC}"
    echo ""
fi

# Exit code basado en resultados
if [ $failed_tests -eq 0 ]; then
    exit 0
else
    exit 1
fi
