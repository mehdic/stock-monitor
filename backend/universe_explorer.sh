#!/bin/bash

# Universe Explorer - Interactive Planet Browser
# Compatible with Bash 3.2+ (macOS default)

set -e

# Color codes for better UX
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Clear screen function
clear_screen() {
    clear
    echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║          🌌 UNIVERSE EXPLORER v1.0 🌌                 ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Function to get planets for a given galaxy and star
get_planets() {
    local galaxy="$1"
    local star="$2"

    case "$galaxy|$star" in
        "Milky Way Galaxy|Sol")
            echo "Mercury,Venus,Earth,Mars,Jupiter,Saturn,Uranus,Neptune"
            ;;
        "Milky Way Galaxy|Proxima Centauri")
            echo "Proxima b,Proxima c,Proxima d"
            ;;
        "Milky Way Galaxy|TRAPPIST-1")
            echo "TRAPPIST-1b,TRAPPIST-1c,TRAPPIST-1d,TRAPPIST-1e,TRAPPIST-1f,TRAPPIST-1g,TRAPPIST-1h"
            ;;
        "Milky Way Galaxy|Kepler-452")
            echo "Kepler-452b (Super-Earth)"
            ;;
        "Milky Way Galaxy|Betelgeuse")
            echo "No confirmed planets (red supergiant star)"
            ;;
        "Andromeda Galaxy|PA-99-N2")
            echo "PA-99-N2 b (candidate exoplanet)"
            ;;
        "Andromeda Galaxy|Alpheratz")
            echo "No confirmed planets"
            ;;
        "Andromeda Galaxy|Mirach")
            echo "No confirmed planets"
            ;;
        "Andromeda Galaxy|Almach")
            echo "No confirmed planets"
            ;;
        "Triangulum Galaxy|HD 12545")
            echo "HD 12545 b (hypothetical)"
            ;;
        "Triangulum Galaxy|NGC 604 Region")
            echo "Unknown systems (too distant)"
            ;;
        "Triangulum Galaxy|M33 Central Core")
            echo "Supermassive black hole region"
            ;;
        "Sombrero Galaxy|M104 Core Stars")
            echo "Too distant for planet detection"
            ;;
        "Sombrero Galaxy|Halo Region Stars")
            echo "Unknown"
            ;;
        "Whirlpool Galaxy|M51 Spiral Arms")
            echo "Unknown planetary systems"
            ;;
        "Whirlpool Galaxy|NGC 5195 Companion")
            echo "Unknown"
            ;;
        *)
            echo "No data available"
            ;;
    esac
}

# Function to get stars for a given galaxy
get_stars() {
    local galaxy="$1"

    case "$galaxy" in
        "Milky Way Galaxy")
            echo "Sol|Proxima Centauri|TRAPPIST-1|Kepler-452|Betelgeuse"
            ;;
        "Andromeda Galaxy")
            echo "PA-99-N2|Alpheratz|Mirach|Almach"
            ;;
        "Triangulum Galaxy")
            echo "HD 12545|NGC 604 Region|M33 Central Core"
            ;;
        "Sombrero Galaxy")
            echo "M104 Core Stars|Halo Region Stars"
            ;;
        "Whirlpool Galaxy")
            echo "M51 Spiral Arms|NGC 5195 Companion"
            ;;
        *)
            echo ""
            ;;
    esac
}

# Function to display galaxies
show_galaxies() {
    clear_screen
    echo -e "${YELLOW}Available Galaxies:${NC}"
    echo ""
    echo "  1) Milky Way Galaxy (our home)"
    echo "  2) Andromeda Galaxy (M31)"
    echo "  3) Triangulum Galaxy (M33)"
    echo "  4) Sombrero Galaxy (M104)"
    echo "  5) Whirlpool Galaxy (M51)"
    echo ""
    echo "  0) Exit"
    echo ""
}

# Function to display stars from selected galaxy
show_stars() {
    local galaxy=$1
    clear_screen
    echo -e "${GREEN}Selected Galaxy: ${galaxy}${NC}"
    echo ""
    echo -e "${YELLOW}Available Stars:${NC}"
    echo ""

    local stars=$(get_stars "$galaxy")
    IFS='|' read -ra STAR_ARRAY <<< "$stars"

    local counter=1
    for star in "${STAR_ARRAY[@]}"; do
        echo "  ${counter}) ${star}"
        ((counter++))
    done

    echo ""
    echo "  0) Back to galaxies"
    echo ""
}

# Function to display planets for selected star
show_planets() {
    local galaxy=$1
    local star=$2

    clear_screen
    echo -e "${GREEN}Galaxy: ${galaxy}${NC}"
    echo -e "${BLUE}Star: ${star}${NC}"
    echo ""
    echo -e "${YELLOW}Planets/Bodies:${NC}"
    echo ""

    local planets=$(get_planets "$galaxy" "$star")

    if [ -z "$planets" ] || [ "$planets" = "No data available" ]; then
        echo -e "${RED}  No data available for this star.${NC}"
    else
        IFS=',' read -ra PLANET_ARRAY <<< "$planets"
        for planet in "${PLANET_ARRAY[@]}"; do
            echo -e "  ${CYAN}🪐${NC} ${planet}"
        done
    fi

    echo ""
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read -r
}

# Main loop
main() {
    while true; do
        show_galaxies
        read -p "Select a galaxy (0-5): " galaxy_choice

        case $galaxy_choice in
            0)
                echo -e "${GREEN}Thank you for exploring the universe! 🚀${NC}"
                exit 0
                ;;
            1) selected_galaxy="Milky Way Galaxy" ;;
            2) selected_galaxy="Andromeda Galaxy" ;;
            3) selected_galaxy="Triangulum Galaxy" ;;
            4) selected_galaxy="Sombrero Galaxy" ;;
            5) selected_galaxy="Whirlpool Galaxy" ;;
            *)
                echo -e "${RED}Invalid choice. Please try again.${NC}"
                sleep 2
                continue
                ;;
        esac

        # Star selection loop
        while true; do
            show_stars "$selected_galaxy"
            read -p "Select a star (0 to go back): " star_choice

            if [ "$star_choice" = "0" ]; then
                break
            fi

            # Get the stars for this galaxy
            local stars=$(get_stars "$selected_galaxy")
            IFS='|' read -ra STAR_ARRAY <<< "$stars"

            # Get the selected star name
            if [ "$star_choice" -ge 1 ] && [ "$star_choice" -le "${#STAR_ARRAY[@]}" ]; then
                selected_star="${STAR_ARRAY[$((star_choice-1))]}"
                show_planets "$selected_galaxy" "$selected_star"
            else
                echo -e "${RED}Invalid star selection.${NC}"
                sleep 2
            fi
        done
    done
}

# Run the main program
main
