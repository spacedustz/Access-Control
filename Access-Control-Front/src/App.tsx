import './App.css'
import React from "react";
import styled from "styled-components";
import TripWireCrossing from "./components/TripWireCrossing";

// Main App Styled Component
const AppBlock = styled.div`
  width: 512px;
  margin: 0 auto;
  margin-top: 4rem;
  border: 1px solid black;
  padding: 1rem;
`;

const App: React.FC = () => {

    return (
        <div>
            <AppBlock>
                <TripWireCrossing />
            </AppBlock>
        </div>
    );
}

export default App